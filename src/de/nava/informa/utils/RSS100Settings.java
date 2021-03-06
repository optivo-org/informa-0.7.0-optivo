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


// $Id: RSS100Settings.java,v 1.5 2006/12/04 23:43:27 italobb Exp $

package de.nava.informa.utils;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ChannelUpdatePeriod;

/**
 * @author  Jean-Guy Avelin
 */
public class RSS100Settings implements CacheSettingsIF {

  //private static Log logger = LogFactory.getLog(RSS100Settings.class);

  private long defaultTtl = DEFAULT_TTL;

  public void setDefaultTtl(long defaultTtl) {
    this.defaultTtl = defaultTtl;
  }

  /**
   * Returns the ttl (in order of preference) feed producer ttl (if exists)
   * wantedTtl (if exists) defaultTtl (if exists).
   */
  public long getTtl(ChannelIF channel, long ttlInMs) {
    ChannelUpdatePeriod updatePeriod = channel.getUpdatePeriod();
    int updateFrequency = channel.getUpdateFrequency();

    if (updatePeriod == null) {
      if (ttlInMs > MINIMAL_TTL)
        return ttlInMs;
      return this.defaultTtl;
    }

    if (updateFrequency < 0) {
      updateFrequency = 1;
    }

    long timeToExpire = updatePeriod.getMsInPeriod() / updateFrequency;

    if (ttlInMs > timeToExpire) {
      return ttlInMs;
    }
    return timeToExpire;
  }

}