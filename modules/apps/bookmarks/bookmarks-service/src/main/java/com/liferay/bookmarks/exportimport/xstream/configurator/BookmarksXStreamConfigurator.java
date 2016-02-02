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

package com.liferay.bookmarks.exportimport.xstream.configurator;

import com.liferay.bookmarks.model.impl.BookmarksEntryImpl;
import com.liferay.bookmarks.model.impl.BookmarksFolderImpl;
import com.liferay.exportimport.xstream.configurator.XStreamConfigurator;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portlet.exportimport.xstream.XStreamAlias;
import com.liferay.portlet.exportimport.xstream.XStreamConverter;
import com.liferay.portlet.exportimport.xstream.XStreamType;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * @author Mate Thurzo
 */
@Component(immediate = true, service = XStreamConfigurator.class)
public class BookmarksXStreamConfigurator implements XStreamConfigurator {

	@Override
	public List<XStreamType> getAllowedXStreamTypes() {
		return null;
	}

	@Override
	public List<XStreamAlias> getXStreamAliases() {
		return ListUtil.toList(_xStreamAliases);
	}

	@Override
	public List<XStreamConverter> getXStreamConverters() {
		return null;
	}

	@Activate
	protected void activate() {
		_xStreamAliases = new XStreamAlias[] {
			new XStreamAlias(BookmarksEntryImpl.class, "BookmarksEntry"),
			new XStreamAlias(BookmarksFolderImpl.class, "BookmarksFolder")
		};
	}

	private XStreamAlias[] _xStreamAliases;

}