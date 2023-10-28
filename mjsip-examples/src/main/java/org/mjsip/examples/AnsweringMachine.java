/*
 * Copyright (C) 2007 Luca Veltri - University of Parma - Italy
 * 
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package org.mjsip.examples;



import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.mjsip.media.MediaDesc;
import org.mjsip.sip.address.NameAddress;
import org.mjsip.sip.message.SipMessage;
import org.mjsip.sip.provider.SipConfig;
import org.mjsip.sip.provider.SipOptions;
import org.mjsip.sip.provider.SipProvider;
import org.mjsip.sip.provider.SipStack;
import org.mjsip.time.Scheduler;
import org.mjsip.time.SchedulerConfig;
import org.mjsip.ua.MediaConfig;
import org.mjsip.ua.MultipleUAS;
import org.mjsip.ua.PortConfig;
import org.mjsip.ua.PortPool;
import org.mjsip.ua.RegistrationOptions;
import org.mjsip.ua.ServiceConfig;
import org.mjsip.ua.UAConfig;
import org.mjsip.ua.UAOptions;
import org.mjsip.ua.UserAgent;
import org.mjsip.ua.UserAgentListener;
import org.mjsip.ua.UserAgentListenerAdapter;
import org.mjsip.ua.streamer.StreamerFactory;
import org.slf4j.LoggerFactory;
import org.zoolu.util.Flags;



/**
 * {@link AnsweringMachine} is a VOIP server that automatically accepts incoming calls, sends an audio file and records
 * input received from the remote end.
 */
public class AnsweringMachine extends MultipleUAS {
	
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AnsweringMachine.class);

	/** Media file to play when answering the call. */
	public static String DEFAULT_ANNOUNCEMENT_FILE="./announcement-8000hz-mono-a-law.wav";

	private MediaConfig _mediaConfig;

	private PortPool _portPool;

	/** 
	 * Creates an {@link AnsweringMachine}. 
	 */
	public AnsweringMachine(SipProvider sip_provider, StreamerFactory streamerFactory, RegistrationOptions regOptions, UAOptions uaConfig, MediaConfig mediaConfig, PortPool portPool, ServiceConfig serviceConfig) {
		super(sip_provider,streamerFactory, regOptions, uaConfig, serviceConfig);
		_mediaConfig = mediaConfig;
		_portPool = portPool;
	}
	
	@Override
	protected UserAgentListener createCallHandler(SipMessage msg) {
		MediaConfig callMedia = MediaConfig.from(_mediaConfig.mediaDescs);
		callMedia.allocateMediaPorts(_portPool);
		
		return new UserAgentListenerAdapter() {
			@Override
			public void onUaIncomingCall(UserAgent ua, NameAddress callee, NameAddress caller, MediaDesc[] media_descs) {
				LOG.info("Incomming call from: " + callee.getAddress());
				ua.accept(callMedia.mediaDescs);
			}
			
			@Override
			public void onUaCallClosed(UserAgent ua) {
				LOG.info("Call closed.");
				callMedia.releaseMediaPorts(_portPool);
			}
		};
	}

	/** 
	 * The main entry point. 
	 */
	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		String program = AnsweringMachine.class.getSimpleName();
		LOG.info(program + " " + SipStack.version);

		Flags flags=new Flags(program, args);
		String config_file=flags.getString("-f","<file>", System.getProperty("user.home") + "/.mjsip-ua" ,"loads configuration from the given file");
		SipOptions sipConfig = SipConfig.init(config_file, flags);
		UAConfig uaConfig = UAConfig.init(config_file, flags, sipConfig);
		SchedulerConfig schedulerConfig = SchedulerConfig.init(config_file);
		MediaConfig mediaConfig = MediaConfig.init(config_file, flags);
		PortConfig portConfig = PortConfig.init(config_file, flags);
		ServiceConfig serviceConfig=ServiceConfig.init(config_file, flags);         
		flags.close();
		
		if (mediaConfig.sendFile != null) {
			AudioFileFormat audioFormat = AudioSystem.getAudioFileFormat(new File(mediaConfig.sendFile));
			LOG.info("Announcement file format: " + audioFormat);
		}
		
		PortPool portPool = new PortPool(portConfig.mediaPort, portConfig.portCount);
		StreamerFactory streamerFactory = mediaConfig.createStreamerFactory(uaConfig);
		SipProvider sipProvider = new SipProvider(sipConfig, new Scheduler(schedulerConfig));
		new AnsweringMachine(sipProvider, streamerFactory, uaConfig, uaConfig, mediaConfig, portPool, serviceConfig);
	}    

}