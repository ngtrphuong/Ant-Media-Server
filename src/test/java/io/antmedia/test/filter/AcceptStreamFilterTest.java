package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.filters.CorsFilter;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.scope.WebScope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.CorsHeaderFilter;
import io.antmedia.filter.StreamAcceptFilter;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;

@ContextConfiguration(locations = {"../test.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AcceptStreamFilterTest extends AbstractJUnit4SpringContextTests {
	
	private StreamAcceptFilter acceptStreamFilter;
	private AppSettings appSettings;
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	@Before
	public void before() {
		acceptStreamFilter = new StreamAcceptFilter();
		
		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}


		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}
	
	@After
	public void after() {
		acceptStreamFilter = null;

		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}
	
	
	@Test
	public void testAcceptFilter() {
		
		StreamAcceptFilter acceptStreamFilterSpy = Mockito.spy(acceptStreamFilter);
		
		AppSettings appSettings = new AppSettings();
		
		AVFormatContext inputFormatContext = null;
		AVPacket pkt = null;
		String streamId = "test-gg";
		
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(AntMediaApplicationAdapter.class);
		
		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdapter.setDataStoreFactory(dsf);
		
		Mockito.doReturn(dataStore).when(acceptStreamFilterSpy).getDataStore();
		Mockito.doReturn(appSettings).when(acceptStreamFilterSpy).getAppSettings();
		
		assertEquals(0,acceptStreamFilterSpy.getMaxFps());
		assertEquals(0,acceptStreamFilterSpy.getMaxResolution());
		assertEquals(0,acceptStreamFilterSpy.getMaxBitrate());
		
		Mockito.doReturn(30).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(2000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));
		
		// Default Scenario
		
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));		
		
		// Stream FPS > Max FPS Scenario
		
		Mockito.doReturn(30).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));		
		
		// Stream Resolution > Max Resolution Scenario
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(480).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Stream Bitrate > Max Bitrate Scenario
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Stream Bitrate > Max Bitrate Scenario && getMaxResolutionAccept = null
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Normal Scenario & getMaxBitrateAccept = null
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Normal Scenario & getMaxFpsAccept = null & getMaxBitrateAccept = null
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());

		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// For the Stream Planned Start / End Data Parameters Scenarios
		// Normal Scenario Stream Parameters which are getMaxFpsAccept = null & getMaxResolution = null & getMaxBitrateAccept = null 
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
		Broadcast broadcast = new Broadcast();
		
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		dataStore.save(broadcast);
		
		// Scenario-1
		// Broadcasts getPlannedStartDate = null & getPlannedEndDate = null
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Scenario-2
		// Broadcast getPlannedStartDate > now & getPlannedEndDate > now
		broadcast.setPlannedStartDate(9999999999l);
		broadcast.setPlannedEndDate(9999999999l);
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Scenario-3
		// Broadcast getPlannedStartDate < now & getPlannedEndDate > now
		broadcast.setPlannedStartDate(99l);
		broadcast.setPlannedEndDate(9999999999l);
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Scenario-4
		// Broadcast getPlannedStartDate > now & getPlannedEndDate < now
		broadcast.setPlannedStartDate(9999999999l);
		broadcast.setPlannedEndDate(99l);
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Scenario-5
		// Broadcast getPlannedStartDate < now & getPlannedEndDate < now
		broadcast.setPlannedStartDate(99l);
		broadcast.setPlannedEndDate(99l);
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Scenario-6
		// Broadcast getPlannedStartDate = null & getPlannedEndDate < now
		broadcast.setPlannedStartDate(null);
		broadcast.setPlannedEndDate(99l);
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));	
		
		// Scenario-7
		// Broadcast getPlannedStartDate > now & getPlannedEndDate = null
		broadcast.setPlannedStartDate(9999999999l);
		broadcast.setPlannedEndDate(null);
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt,streamId));

	}
	
	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

}
