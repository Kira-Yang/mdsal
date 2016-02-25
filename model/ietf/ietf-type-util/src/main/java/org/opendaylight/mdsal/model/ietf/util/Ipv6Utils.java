/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.model.ietf.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.InetAddress;
import javax.annotation.Nonnull;

/**
 * IPv6 address parsing for ietf-inet-types ipv6-address and ipv6-prefix. This is an internal implementation
 * class, not meant to be exposed in any shape or form to the outside world, as the code relies on the fact that
 * the strings presented to it have been previously validated to conform to the regular expressions defined in
 * the YANG model.
 */
/*
 * v6 routines added by Anton Ivanov on 14.6.2015
 * revised by Robert Varga
 *
 * BIG FAT WARNING!!!
 * Read all of the following before you touch any v6 code or decide to
 * optimize it by invoking a "simple" Guava call
 *
 * Java IPv6 is fundamentally broken and Google libraries do not fix it.
 * 1. Java will allways implicitly rewrite v4 mapped into v6 as a v4 address
 *      and there is absolutely no way to override this behaviour
 * 2. Guava libraries cannot parse non-canonical IPv6. They will throw an
 *      exception. Even if they did, they re-use the same broken java code
 *      underneath.
 *
 * This is why we have to parse v6 by ourselves.
 *
 * The following conversion code is based on inet_cidr_pton_ipv6 in NetBSD
 *
 * The original BSD code is licensed under standard BSD license. While we
 * are not obliged to provide an attribution, credit where credit is due.
 * As far as why it is similar to Sun's sun.net.util please ask Sun why
 * their code has the same variable names, comments and code flow.
 */
final class Ipv6Utils {
    private static final int INADDR4SZ = 4;
    private static final int INADDR6SZ = 16;
    private static final int INT16SZ = Short.BYTES;

    private Ipv6Utils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Convert Ipv6Address object to a valid Canonical v6 address in byte format
     *
     * @param ipv6Address - v6 Address object
     *
     * FIXME: rovarga: this looks wrong
     * @return - byte array of size 16. Last byte contains netmask
     *
     * @throws NullPointerException if ipv6address is null
     */
   public static @Nonnull byte[] bytesForString(final @Nonnull String ipv6Address) {
       final int percentPos = ipv6Address.indexOf('%');
       // FIXME: do not perform a copy, just set the limit here.
       final String address = percentPos == -1 ? ipv6Address : ipv6Address.substring(0, percentPos);

       /* Leading :: requires some special handling. */
       int i = 0;
       if (address.charAt(i) == ':') {
           // Note ++i side-effect in check
           Preconditions.checkArgument(address.charAt(++i) == ':', "Invalid v6 address '%s'", ipv6Address);
       }

       final byte[] dst = new byte[INADDR6SZ];

       final int src_length = address.length();
       boolean saw_xdigit = false;
       int val = 0;
       int colonp = -1;
       int j = 0;
       int curtok = i;
       while (i < src_length) {
           final char ch = address.charAt(i++);

           /* v6 separator */
           if (ch == ':') {
               curtok = i;
               if (!saw_xdigit) {
                   /* no need to check separator position validity - regexp does that */
                   colonp = j;
                   continue;
               }

               /* removed overrun check - the regexp checks for valid data */

               dst[j++] = (byte) ((val >>> 8) & 0xff);
               dst[j++] = (byte) (val & 0xff);
               saw_xdigit = false;
               val = 0;
               continue;
           }

           /* frankenstein - v4 attached to v6, mixed notation */
           if (ch == '.' && ((j + INADDR4SZ) <= INADDR6SZ)) {

               /* this has passed the regexp so it is fairly safe to parse it
                * straight away. As v4 addresses do not suffer from the same
                * deficiencies as the java v6 implementation we can invoke it
                * straight away and be done with it
                */
               Preconditions.checkArgument(j != (INADDR6SZ - INADDR4SZ - 1), "Invalid v4 in v6 mapping in %s", ipv6Address);
               InetAddress _inet_form = InetAddresses.forString(address.substring(curtok, src_length));

               Preconditions.checkArgument(_inet_form instanceof Inet4Address);
               System.arraycopy(_inet_form.getAddress(), 0, dst, j, INADDR4SZ);
               j += INADDR4SZ;

               saw_xdigit = false;
               break;
           }

           /* Business as usual - ipv6 address digit.
            * We can remove all checks from the original BSD code because
            * the regexp has already verified that we are not being fed
            * anything bigger than 0xffff between the separators.
            */
           final int chval = AbstractIetfYangUtil.hexValue(ch);
           val = (val << 4) | chval;
           saw_xdigit = true;
       }

       if (saw_xdigit) {
           Verify.verify(j + INT16SZ <= INADDR6SZ, "Overrun in parsing of '%s', should not occur", ipv6Address);
           dst[j++] = (byte) ((val >> 8) & 0xff);
           dst[j++] = (byte) (val & 0xff);
       }

       if (colonp != -1) {
           Verify.verify(j != INADDR6SZ, "Overrun in parsing of '%s', should not occur", ipv6Address);

           final int n = j - colonp;
           for (i = 1; i <= n; i++) {
               dst[INADDR6SZ - i] = dst[colonp + n - i];
               dst[colonp + n - i] = 0;
           }
           j = INADDR6SZ;
       }

       Verify.verify(j == INADDR6SZ, "Overrun in parsing of '%s', should not occur", ipv6Address);
       return dst;
   }
}