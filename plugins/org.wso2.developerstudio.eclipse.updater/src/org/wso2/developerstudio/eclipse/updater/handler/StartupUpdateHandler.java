/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.developerstudio.eclipse.updater.handler;

import java.util.Calendar;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;
import org.wso2.developerstudio.eclipse.platform.ui.WorkbenchToolkit;
import org.wso2.developerstudio.eclipse.platform.ui.preferences.PreferenceConstants;
import org.wso2.developerstudio.eclipse.platform.ui.preferences.PreferenceInitializer;
import org.wso2.developerstudio.eclipse.platform.ui.preferences.UpdateCheckerPreferencePage;
import org.wso2.developerstudio.eclipse.updater.UpdaterPlugin;
import org.wso2.developerstudio.eclipse.updater.core.UpdateManager;
import org.wso2.developerstudio.eclipse.updater.job.BackgroundUpdateTaskJob;
import org.wso2.developerstudio.eclipse.updater.job.BackgroundUpdaterTaskListener;
import org.wso2.developerstudio.eclipse.updater.job.UpdateMetaFileReaderJob;
import org.wso2.developerstudio.eclipse.updater.job.UpdateMetaFileReaderJobListener;
import org.wso2.developerstudio.eclipse.updater.ui.UpdaterDialog.ActiveTab;

public class StartupUpdateHandler implements IStartup {

	private static final int DELAY_ONE_MINUTE = 60000;
	protected static final String DAILY = "Daily";
	protected static final String WEEKLY = "Weekly";
	protected static final String MONTHLY = "Monthly";
	protected long repeatDelay = 3600000;
	protected long minute = 60000;

	protected UpdateManager updateManager = new UpdateManager();

	protected static IDeveloperStudioLog log = Logger.getLog(UpdaterPlugin.PLUGIN_ID);

	@Override
	public void earlyStartup() {
		// check if user has set startup updates
		// Read updater preferences
		// Let updater wait a minute till workspace preferences are initialized
		try {
			Thread.sleep(DELAY_ONE_MINUTE);
		} catch (InterruptedException e) {
			log.error("error while running automatic updates", e);
			Thread.currentThread().interrupt();
		}
		IPreferenceStore prefPage = PlatformUI.getPreferenceStore();
		UpdateCheckerPreferencePage.setPreferenceDefaults(prefPage);
		boolean isAutomaticUpdate = prefPage.getBoolean(PreferenceConstants.ENABLE_AUTOMATIC_UPDATES);
		String updateRunConfig = prefPage.getString(PreferenceConstants.UPDATE_RUNNING_CONFIGURATION);
		if (updateRunConfig == null || updateRunConfig.isEmpty()) {
			updateRunConfig = PreferenceConstants.STARTUP;
		}
		if (updateRunConfig.equals(PreferenceConstants.SCHEDULE)) {
			isAutomaticUpdate = false; // do not run at startup if user has
										// scheduled the updater job
			BackgroundUpdateTaskJob job = new BackgroundUpdateTaskJob("BackgroundScheduler", minute);
			// start at user specified time.
			job.schedule(evaluateTimeToUserScheduledTime());
			job.addJobChangeListener(new BackgroundUpdaterTaskListener(updateManager));
		}
		if (!isAutomaticUpdate) {
			return;
		}
		/**
		 * before running the update checker job, read the updates meta file and
		 * see if it has updates before iterating through the updater
		 * repository. UpdateMetaFileReaderJob
		 */
		runUpdateMetaFileReaderJob();
	}

	private void runUpdateMetaFileReaderJob() {
		Job readMetaFileJob = new UpdateMetaFileReaderJob(updateManager);
		readMetaFileJob.schedule();
		readMetaFileJob.addJobChangeListener(
				new UpdateMetaFileReaderJobListener(updateManager, ActiveTab.UPDATE_FEATURES, true));
	}

	private long evaluateTimeToUserScheduledTime() {
		final IPreferenceStore prefPage = PlatformUI.getPreferenceStore();
		String updateIntervalDay = prefPage.getString(PreferenceConstants.UPDATE_DATE_INTERVAL);
		if (updateIntervalDay == null || updateIntervalDay.isEmpty()) {
			updateIntervalDay = PreferenceConstants.DEFAULT_SUNDAY;
		}
		int intValOfDay = getIntValOfDay(updateIntervalDay);
		String updateIntervalTime = prefPage.getString(PreferenceConstants.UPDATE_TIME_INTERVAL);
		if (updateIntervalTime == null || updateIntervalTime.isEmpty()) {
			updateIntervalTime = PreferenceConstants.DEFAULT_EIGHT_AM;
		}

		String[] hourValFromIntervalTime = updateIntervalTime.split(":");
		Integer intHourVal = Integer.parseInt(hourValFromIntervalTime[0]);

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_WEEK, intValOfDay);
		c.set(Calendar.HOUR_OF_DAY, intHourVal);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return (c.getTimeInMillis() - System.currentTimeMillis());

	}

	private int getIntValOfDay(String updateIntervalDay) {
		switch (updateIntervalDay) {
		case (PreferenceConstants.EVERY_MONDAY):
			return Calendar.MONDAY;
		case (PreferenceConstants.EVERY_TUESDAY):
			return Calendar.TUESDAY;
		case (PreferenceConstants.EVERY_WEDNESDAY):
			return Calendar.WEDNESDAY;
		case (PreferenceConstants.EVERY_THURSDAY):
			return Calendar.THURSDAY;
		case (PreferenceConstants.EVERY_FRIDAY):
			return Calendar.FRIDAY;
		case (PreferenceConstants.EVERY_SATURDAY):
			return Calendar.SATURDAY;
		default:
			return Calendar.SUNDAY; // case SUNDAY default setting
		}

	}
}
