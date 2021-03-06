//
// Informa -- RSS Library for Java
// Copyright (c) 2002 by Niko Schmuck
//
// Niko Schmuck
// http://sourceforge.net/projects/informa
// mailto:niko_schmuck@users.sourceforge.net
//
// This library is free software.
//
// You may redistribute it and/or modify it under the terms of the GNU
// Lesser General Public License as published by the Free Software Foundation.
//
// Version 2.1 of the license should be included with this distribution in
// the file LICENSE. If the license is not included with this distribution,
// you may find a copy at the FSF web site at 'www.gnu.org' or 'www.fsf.org',
// or you may write to the Free Software Foundation, 675 Mass Ave, Cambridge,
// MA 02139 USA.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied waranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//

// $Id: UpdateChannelTask.java,v 1.33 2004/05/13 22:55:17 niko_schmuck Exp $

package de.nava.informa.utils;

import de.nava.informa.core.*;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Date;
import java.util.TimerTask;

/**
 * Class for performing a channel retrieval in periodic intervals as a
 * TimerTask. The existing items of a channel are compared to the
 * items contained in the newly gotten channel, and if there are any
 * new items they are appended.
 *
 * @author Niko Schmuck (niko@nava.de)
 */
public class UpdateChannelTask extends TimerTask {

    private static final Log LOG = LogFactory.getLog(UpdateChannelTask.class);

    private ChannelRegistry registry;
    private ChannelIF channel;
    private ChannelBuilderIF builder;
    private UpdateChannelInfo info;
    private ChannelBuilderIF tempBuilder;

    public UpdateChannelTask(
            ChannelRegistry registry,
            ChannelBuilderIF builder,
            ChannelIF channel,
            UpdateChannelInfo info) {
        this.registry = registry;
        this.channel = channel;
        this.builder = builder;
        this.info = info;

        // builder that is passed in on construction *may* be for persistent Channels.
        // tempBuilder is forced to be memory only so it will not be persisted.
        tempBuilder = new ChannelBuilder();
    }

    public void run() {
        LOG.debug("Task Run()");
        Thread.currentThread().setName("Informa Update Channel Task");
        /**
         * ChannelBuilder is not re-entrant and it is shared by all the
         * UpdateChannelTasks which are created by single ChannelRegistry.
         * Note that all the beginTransaction() must have a corresponding endTransaction()
         */
        synchronized (builder) {
            if (!info.getFormatDetected())
            /**
             * If this is the first time we see this Channel, then we will now attempt
             * to parse it and if this works we remember the format and proceed.
             * Otherwise we trigger error case handling and eventually deactivate it.
             */ {
                try {
                    builder.beginTransaction();
                    ChannelFormat format =
                            FormatDetector.getFormat(channel.getLocation());
                    channel.setFormat(format);
                    info.setFormatDetected(true);
                    channel.setLastUpdated(new Date());
                    builder.endTransaction();
                } catch (UnsupportedFormatException ex) {
                    LOG.debug("Unsupported format for Channel");
                    incrementProblems(ex);
                    return;
                } catch (IOException ioe) {
                    LOG.debug("Cannot retrieve Channel");
                    incrementProblems(ioe);
                    return;
                } catch (ChannelBuilderException ignored) {}
            }
            try {
                synchronized (channel) {
                    builder.beginTransaction();
                    ChannelIF tempChannel =
                            FeedParser.parse(tempBuilder, channel.getLocation());
                    LOG.debug(
                            "Updating channel from "
                                    + channel.getLocation()
                                    + ": "
                                    + tempChannel
                                    + "(new)    "
                                    + channel
                                    + "(old)");
                    InformaUtils.copyChannelProperties(tempChannel, channel);
                    builder.update(channel);
                    channel.setLastUpdated(new Date());
                    // compare with existing items, only add new ones
                    if (tempChannel.getItems().isEmpty()) {
                        LOG.warn("No items found in channel " + channel);
                    } else {
                        for (ItemIF itemIF : tempChannel.getItems()) {
                            if (!channel.getItems().contains(itemIF)) {
                                LOG.debug("Found new item: " + itemIF);
                                channel.addItem(builder.createItem(null, itemIF));
                                //                                                              }
                            }
                        } // while more items
                    }
                    builder.endTransaction();
                }
            } catch (ParseException pe) {
                incrementProblems(pe);
            } catch (IOException ioe) {
                incrementProblems(ioe);
            } catch (ChannelBuilderException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Increases the count of problems occurred so far and put out an error
     * message based on the given exception.
     */
    private void incrementProblems(Exception e) {
        info.increaseProblemsOccurred(e);
        if (info.shouldDeactivate()) {
            LOG.warn(
                    "Deactivating channel after "
                            + info.getNrProblemsOccurred()
                            + " problems occurred.");
            registry.deactivateChannel(channel);
        }
        LOG.warn(e);
    }

}
