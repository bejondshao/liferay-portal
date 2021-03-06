/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.settings;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.resource.manager.ClassLoaderResourceManager;
import com.liferay.portal.kernel.resource.manager.ResourceManager;
import com.liferay.portal.kernel.security.pacl.DoPrivileged;
import com.liferay.portal.kernel.settings.ConfigurationBeanSettings;
import com.liferay.portal.kernel.settings.LocationVariableResolver;
import com.liferay.portal.kernel.settings.PortletPreferencesSettings;
import com.liferay.portal.kernel.settings.PropertiesSettings;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsFactoryUtil;
import com.liferay.portal.kernel.settings.SettingsLocatorHelper;
import com.liferay.portal.kernel.settings.definition.ConfigurationBeanDeclaration;
import com.liferay.portal.kernel.settings.definition.ConfigurationPidMapping;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalPreferencesLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.util.PortletKeys;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.portlet.PortletPreferences;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * @author Iván Zaera
 * @author Jorge Ferrer
 */
@Component(service = SettingsLocatorHelper.class)
@DoPrivileged
public class SettingsLocatorHelperImpl implements SettingsLocatorHelper {

	public PortletPreferences getCompanyPortletPreferences(
		long companyId, String settingsId) {

		return PortletPreferencesLocalServiceUtil.getStrictPreferences(
			companyId, companyId, PortletKeys.PREFS_OWNER_TYPE_COMPANY, 0,
			settingsId);
	}

	@Override
	public Settings getCompanyPortletPreferencesSettings(
		long companyId, String settingsId, Settings parentSettings) {

		return new PortletPreferencesSettings(
			getCompanyPortletPreferences(companyId, settingsId),
			parentSettings);
	}

	@Override
	public Settings getConfigurationBeanSettings(
		String settingsId, Settings parentSettings) {

		return new ConfigurationBeanSettings(
			new LocationVariableResolver(
				getResourceManager(settingsId),
				SettingsFactoryUtil.getSettingsFactory()),
			getConfigurationBean(settingsId), parentSettings);
	}

	public PortletPreferences getGroupPortletPreferences(
		long groupId, String settingsId) {

		try {
			Group group = GroupLocalServiceUtil.getGroup(groupId);

			return PortletPreferencesLocalServiceUtil.getStrictPreferences(
				group.getCompanyId(), groupId,
				PortletKeys.PREFS_OWNER_TYPE_GROUP, 0, settingsId);
		}
		catch (PortalException pe) {
			throw new SystemException(pe);
		}
	}

	@Override
	public Settings getGroupPortletPreferencesSettings(
		long groupId, String settingsId, Settings parentSettings) {

		return new PortletPreferencesSettings(
			getGroupPortletPreferences(groupId, settingsId), parentSettings);
	}

	public PortletPreferences getPortalPreferences(long companyId) {
		return PortalPreferencesLocalServiceUtil.getPreferences(
			companyId, PortletKeys.PREFS_OWNER_TYPE_COMPANY);
	}

	@Override
	public Settings getPortalPreferencesSettings(
		long companyId, Settings parentSettings) {

		return new PortletPreferencesSettings(
			getPortalPreferences(companyId), parentSettings);
	}

	public Properties getPortalProperties() {
		return PropsUtil.getProperties();
	}

	@Override
	public Settings getPortalPropertiesSettings() {
		return new PropertiesSettings(
			new LocationVariableResolver(
				new ClassLoaderResourceManager(
					PortalClassLoaderUtil.getClassLoader()),
				SettingsFactoryUtil.getSettingsFactory()),
			getPortalProperties());
	}

	public PortletPreferences getPortletInstancePortletPreferences(
		long companyId, long plid, String portletId) {

		long ownerId = PortletKeys.PREFS_OWNER_ID_DEFAULT;
		int ownerType = PortletKeys.PREFS_OWNER_TYPE_LAYOUT;

		if (PortletConstants.hasUserId(portletId)) {
			ownerId = PortletConstants.getUserId(portletId);
			ownerType = PortletKeys.PREFS_OWNER_TYPE_USER;
		}

		return PortletPreferencesLocalServiceUtil.getStrictPreferences(
			companyId, ownerId, ownerType, plid, portletId);
	}

	@Override
	public Settings getPortletInstancePortletPreferencesSettings(
		long companyId, long plid, String portletId, Settings parentSettings) {

		return new PortletPreferencesSettings(
			getPortletInstancePortletPreferences(companyId, plid, portletId),
			parentSettings);
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;

		for (ConfigurationBeanManagedService configurationBeanManagedService :
				_configurationBeanManagedServices.values()) {

			configurationBeanManagedService.setBundleContext(bundleContext);

			configurationBeanManagedService.register();
		}
	}

	protected Object getConfigurationBean(String settingsId) {
		settingsId = PortletConstants.getRootPortletId(settingsId);

		Class<?> configurationBeanClass = _configurationBeanClasses.get(
			settingsId);

		if (configurationBeanClass == null) {
			return null;
		}

		ConfigurationBeanManagedService configurationBeanManagedService =
			_configurationBeanManagedServices.get(configurationBeanClass);

		return configurationBeanManagedService.getConfigurationBean();
	}

	protected ResourceManager getResourceManager(String settingsId) {
		settingsId = PortletConstants.getRootPortletId(settingsId);

		Class<?> configurationBeanClass = _configurationBeanClasses.get(
			settingsId);

		if (configurationBeanClass == null) {
			return new ClassLoaderResourceManager(
				PortalClassLoaderUtil.getClassLoader());
		}

		return new ClassLoaderResourceManager(
			configurationBeanClass.getClassLoader());
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void setConfigurationBeanDeclaration(
		ConfigurationBeanDeclaration configurationBeanDeclaration) {

		Class<?> configurationBeanClass =
			configurationBeanDeclaration.getConfigurationBeanClass();

		if (_configurationBeanManagedServices.containsKey(
				configurationBeanClass)) {

			if (_log.isWarnEnabled()) {
				_log.warn(
					"Configuration bean declaration for configuration bean " +
						configurationBeanClass.getName() + " already exists");
			}

			return;
		}

		ConfigurationBeanManagedService configurationBeanManagedService =
			new ConfigurationBeanManagedService(
				_bundleContext, configurationBeanClass);

		_configurationBeanClasses.put(
			configurationBeanManagedService.getConfigurationPid(),
			configurationBeanClass);
		_configurationBeanManagedServices.put(
			configurationBeanClass, configurationBeanManagedService);

		configurationBeanManagedService.register();
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void setConfigurationPidMapping(
		ConfigurationPidMapping configurationPidMapping) {

		_configurationBeanClasses.put(
			configurationPidMapping.getConfigurationPid(),
			configurationPidMapping.getConfigurationBeanClass());
	}

	protected void unsetConfigurationBeanDeclaration(
		ConfigurationBeanDeclaration configurationBeanDeclaration) {

		Class<?> configurationBeanClass =
			configurationBeanDeclaration.getConfigurationBeanClass();

		ConfigurationBeanManagedService configurationBeanManagedService =
			_configurationBeanManagedServices.get(configurationBeanClass);

		configurationBeanManagedService.unregister();

		_configurationBeanClasses.remove(
			configurationBeanManagedService.getConfigurationPid());
		_configurationBeanManagedServices.remove(configurationBeanClass);
	}

	protected void unsetConfigurationPidMapping(
		ConfigurationPidMapping configurationPidMapping) {

		_configurationBeanClasses.remove(
			configurationPidMapping.getConfigurationPid());
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SettingsLocatorHelperImpl.class);

	private BundleContext _bundleContext;
	private final ConcurrentMap<String, Class<?>> _configurationBeanClasses =
		new ConcurrentHashMap<>();
	private final ConcurrentMap<Class<?>, ConfigurationBeanManagedService>
		_configurationBeanManagedServices = new ConcurrentHashMap<>();

}