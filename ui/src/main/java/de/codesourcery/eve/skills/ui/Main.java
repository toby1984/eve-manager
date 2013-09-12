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
package de.codesourcery.eve.skills.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.ui.components.impl.planning.CalendarReminder;
import de.codesourcery.eve.skills.ui.config.AppConfig;
import de.codesourcery.eve.skills.ui.config.IAppConfigChangeListener;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.frames.WindowManager;
import de.codesourcery.eve.skills.ui.frames.impl.MainFrame;
import de.codesourcery.eve.skills.ui.utils.ApplicationThreadManager;
import de.codesourcery.eve.skills.ui.utils.SpringUtil;

public class Main implements IMain, IAppConfigChangeListener
{
    private static final Logger log = Logger.getLogger( Main.class );

    private ApplicationThreadManager threadManager;

    private IMarketDataProvider marketDataProvider;
    private IAppConfigProvider configProvider;
    private volatile IUserAccountStore userAccountStore;
    private IAPIClient apiClient;
    private volatile MainFrame frame;
    private CalendarReminder calendarReminder;

    private volatile boolean shutdownCalled = false;

    public Main() throws IOException {
    }

    public void setCalendarReminder(CalendarReminder calendarReminder)
    {
        this.calendarReminder = calendarReminder;
    }

    public void setConfigProvider(IAppConfigProvider configProvider)
    {
        this.configProvider = configProvider;
    }

    public void setApiClient(IAPIClient apiClient)
    {
        this.apiClient = apiClient;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.codesourcery.eve.skills.ui.IMain#shutdown()
     */
    public synchronized void shutdown()
    {

        if ( shutdownCalled )
        {
            log.info( "shutdown(): Shutdown already started." );
            return;
        }

        log.info( "shutdown(): Application shutdown started..." );

        shutdownCalled = true;

        try
        {

            try
            {
                if ( threadManager != null )
                {
                    log.info( "shutdown(): Stopping application threads." );
                    threadManager.shutdown( true );
                }
            }
            catch (Exception e)
            {
                log.error( "shutdown(): Caught exception during shutdown ", e );
            }

            try
            {
                log.info( "shutdown(): Destroying application windows." );
                WindowManager.getInstance().shutdown( true );
            }
            catch (Exception e)
            {
                log.error( "shutdown(): Caught exception during shutdown ", e );
            }

            try
            {
                if ( configProvider != null )
                {
                    configProvider.removeChangeListener( this );
                    configProvider.save();
                }
            }
            catch (Exception e)
            {
                log.error( "shutdown(): Caught exception during shutdown ", e );
            }

            try
            {
                if ( marketDataProvider != null )
                {
                    log.info( "shutdown(): Destroying market data provider." );
                    marketDataProvider.dispose();
                }
            }
            catch (Exception e)
            {
                log.error( "shutdown(): Caught exception during shutdown ", e );
            }

            try
            {
                if ( userAccountStore != null )
                {
                    log.info( "shutdown(): Persisting user account data." );
                    userAccountStore.persist();
                }
            }
            catch (Exception e)
            {
                log.error( "shutdown(): Caught exception during shutdown ", e );
            }

            try
            {
                if ( apiClient != null )
                {
                    log.info( "shutdown(): Destroying API client." );
                    apiClient.dispose();
                }
            }
            catch (Exception e)
            {
                log.error( "shutdown(): Caught exception during shutdown ", e );
            }

            try
            {
                log.info( "shutdown(): Destroying Spring Context" );
                SpringUtil.getInstance().shutdown();
            }
            catch (Exception e)
            {
                log.error( "shutdown(): Caught exception during shutdown ", e );
            }

        }
        finally
        {
            log.info( "shutdown(): Application shutdown finished." );

            /*
             * TODO: This is a HACK.
             * 
             * Sometimes the EDT prevents orderly shutdown of the VM, may be
             * related to a race-/threading issue that causes stale event
             * listeners to hang around....
             * 
             * Seehttp://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=1
             * cdf30611111631c340b0537750ec?bug_id=4417287 and the like....
             */
            System.exit( 0 );
        }
    }

    public static void main(String[] args) throws Exception
    {

        setLookAndFeel();

        SwingUtilities.invokeAndWait( new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    SpringUtil.getInstance().getMain().startUp();
                }
                catch (Exception e)
                {
                    throw new RuntimeException( e );
                }
            }
        } );
    }

    protected static void setLookAndFeel()
    {

        LookAndFeelInfo newLandF = null;
        for (LookAndFeelInfo lf : UIManager.getInstalledLookAndFeels())
        {
            if ( "Nimbus".equals( lf.getName() ) )
            {
                newLandF = lf;
                break;
            }
        }

        if ( newLandF != null )
        {
            try
            {
                UIManager.setLookAndFeel( newLandF.getClassName() );
            }
            catch (Exception e)
            {
                log.error( "setLookAndFeel(): Failed to change look and feel", e );
            }
        }
        else
        {
            log.info( "setLookAndFeel(): L&F 'Nimbus' not available, using default." );
        }
    }

    public MainFrame getMainFrame()
    {
        return frame;
    }

    public void startUp() throws Exception
    {
        this.configProvider.addChangeListener( this );

        frame = new MainFrame();

        frame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                log.info( "windowClosing(): Main frame closed." );
                frame.dispose();
                shutdown();
            }
        } );

        frame.setVisible( true );

        calendarReminder.startWatchdogThread();
    }

    public void setThreadManager(ApplicationThreadManager manager)
    {
        this.threadManager = manager;
    }

    @Override
    public void appConfigChanged(AppConfig config, String... properties)
    {

        log.info( "appConfigChanged(): Config changed." );

        if ( ArrayUtils.contains( properties, AppConfig.PROP_CLIENT_RETRIEVAL_STRATEGY ) )
        {
            log
                    .info( "appConfigChanged(): Config changed , updating API client retrieval strategy." );
            this.apiClient.setDefaultRetrievalStrategy( config.getClientRetrievalStrategy() );
        }
    }

    public void setMarketDataProvider(IMarketDataProvider marketDataProvider)
    {
        this.marketDataProvider = marketDataProvider;
    }

}
