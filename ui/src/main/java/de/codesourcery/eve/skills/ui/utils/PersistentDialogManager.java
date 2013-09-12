/**
 * Copyright 2004-2009 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.eve.skills.ui.utils;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.swing.Icon;
import javax.swing.UIManager;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.ui.config.AppConfig;
import de.codesourcery.eve.skills.ui.config.IAppConfigChangeListener;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.utils.IPersistentDialog.PersistenceType;
import de.codesourcery.eve.skills.ui.utils.PersistentDialog.Kind;
import de.codesourcery.eve.skills.util.SpringBeanInjector;

public class PersistentDialogManager
{

	@Resource(name = "appconfig-provider")
	private IAppConfigProvider configProvider;

	private final Map<String, Boolean> sessionSettings = new HashMap<String, Boolean>();

	protected interface IPersistenceType
	{

		public PersistenceType getType();

		public void reenableDialog(String id);

		public void rememberDialogState(String id, boolean disabledByUser);

		public boolean isDisabledByUser(String id);

	}

	public PersistentDialogManager() {
		SpringBeanInjector.getInstance().injectDependencies(this);

		configProvider.addChangeListener(new IAppConfigChangeListener() {

			@Override
			public void appConfigChanged(AppConfig config, String... properties)
			{
				if ( ArrayUtils.contains(properties,
						AppConfig.PROP_ALL_DIALOGS_REENABLED) ) {
					reenableAllDialogs();
				}
			}
		});
	}

	private final IPersistenceType PERMANENT_DIALOG = new IPersistenceType() {

		@Override
		public boolean isDisabledByUser(String id)
		{
			return getAppConfig().isDialogDisabledByUser(id);
		}

		@Override
		public void reenableDialog(String id)
		{
			getAppConfig().reenableAllDialogs();
		}

		@Override
		public void rememberDialogState(String id, boolean disabledByUser)
		{
			getAppConfig().rememberDialogSettings(id, disabledByUser);
		}

		@Override
		public PersistenceType getType()
		{
			return PersistenceType.PERMANENT;
		}

	};

	private final IPersistenceType SESSION_DIALOG = new IPersistenceType() {

		@Override
		public boolean isDisabledByUser(String id)
		{
			if ( StringUtils.isBlank(id) ) {
				throw new IllegalArgumentException("id cannot be blank.");
			}

			final Boolean result = sessionSettings.get(id);
			return result != null ? result : false;
		}

		@Override
		public void reenableDialog(String id)
		{

			if ( StringUtils.isBlank(id) ) {
				throw new IllegalArgumentException("id cannot be blank.");
			}

			sessionSettings.remove(id);
		}

		@Override
		public void rememberDialogState(String id, boolean disabledByUser)
		{
			if ( StringUtils.isBlank(id) ) {
				throw new IllegalArgumentException("id cannot be blank.");
			}

			sessionSettings.put(id, disabledByUser);
		}

		@Override
		public PersistenceType getType()
		{
			return PersistenceType.PERMANENT;
		}

	};

	protected AppConfig getAppConfig()
	{
		return configProvider.getAppConfig();
	}

	protected IPersistenceType getTypeFor(IPersistentDialog dialog)
	{
		if ( dialog == null ) {
			throw new IllegalArgumentException("dialog cannot be NULL");
		}
		return getTypeFor(dialog.getPersistenceType());
	}

	protected IPersistenceType getTypeFor(PersistenceType type)
	{

		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}

		switch ( type )
		{
			case SESSION:
				return SESSION_DIALOG;
			case PERMANENT:
				return PERMANENT_DIALOG;
			default:
				throw new RuntimeException("Unhandled persistence type " + type);
		}
	}

	public void rememberDialogSettings(IPersistentDialog dialog)
	{
		if ( dialog == null ) {
			throw new IllegalArgumentException("dialog cannot be NULL");
		}
		getTypeFor(dialog).rememberDialogState(dialog.getId(),
				dialog.isDisabledByUser());
	}

	public boolean isDialogDisabledByUser(IPersistentDialog dialog)
	{
		if ( dialog == null ) {
			throw new IllegalArgumentException("dialog cannot be NULL");
		}
		return getTypeFor(dialog).isDisabledByUser(dialog.getId());
	}

	public boolean isDialogDisabledByUser(String id, PersistenceType type)
	{

		if ( StringUtils.isBlank(id) ) {
			throw new IllegalArgumentException("id cannot be blank.");
		}

		return getTypeFor(type).isDisabledByUser(id);
	}

	public void reenableAllDialogs()
	{
		sessionSettings.clear();
		configProvider.getAppConfig().reenableAllDialogs();
	}

	public void reenableDialog(IPersistentDialog dialog)
	{
		getTypeFor(dialog).reenableDialog( dialog.getId() );
	}

	// ===================

	/**
	 * 
	 * @return <code>true</code> if the user did press the 'ok' button to
	 *         dismiss the dialog, otherwise <code>false</code>
	 */
	public boolean showPermanentWarningDialog(String id, String title,
			String label)
	{

		if ( isDialogDisabledByUser(id, PersistenceType.PERMANENT) ) {
			return true;
		}

		final PersistentDialog dialog = new PersistentDialog(id,
				getMessageIcon(), title, label, PersistenceType.PERMANENT,
				Kind.CANCEL);

		dialog.setVisible(true);
		rememberDialogSettings(dialog);

		return !dialog.wasCancelled();
	}

	/**
	 * 
	 * @return <code>true</code> if the user did press the 'ok' button to
	 *         dismiss the dialog, otherwise <code>false</code>
	 */
	public boolean showTemporaryWarningDialog(String id, String title,
			String label)
	{

		if ( isDialogDisabledByUser(id, PersistenceType.SESSION) ) {
			return true;
		}

		final PersistentDialog dialog = new PersistentDialog(id,
				getMessageIcon(), title, label, PersistenceType.SESSION,
				Kind.CANCEL);

		dialog.setVisible(true);

		rememberDialogSettings(dialog);

		return !dialog.wasCancelled();
	}

	public void showPermanentInfoDialog(String id, String title, String label)
	{

		if ( isDialogDisabledByUser(id, PersistenceType.PERMANENT) ) {
			return;
		}

		final PersistentDialog dialog = new PersistentDialog(id,
				getMessageIcon(), title, label, PersistenceType.PERMANENT,
				Kind.INFO);

		dialog.setVisible(true);
		rememberDialogSettings(dialog);
	}

	public void showTemporaryInfoDialog(String id, String title, String label)
	{

		if ( isDialogDisabledByUser(id, PersistenceType.SESSION) ) {
			return;
		}

		final PersistentDialog dialog = new PersistentDialog(id,
				getMessageIcon(), title, label, PersistenceType.SESSION,
				Kind.INFO);

		dialog.setVisible(true);

		rememberDialogSettings(dialog);
	}

	protected static Icon getWarningIcon()
	{
		return UIManager.getIcon("OptionPane.warningIcon");
	}

	protected static Icon getMessageIcon()
	{
		return UIManager.getIcon("OptionPane.informationIcon");
	}
}
