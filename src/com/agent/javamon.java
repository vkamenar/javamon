// Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// and associated documentation files, to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the Software is furnished
// to do so, subject to the following condition: This copyright and permission notice shall be
// included in all copies or substantial portions of the Software.
// Copyright Vladimir Kamenar, 2025
package com.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

// The Java monitoring agent can be used to monitor heap memory usage and uptime
// for a JVM process. It exposes an HTTP endpoint compatible with Prometheus.
public final class javamon extends Thread{

   private final String host;
   private ServerSocket ss;
   private final int port;
   private boolean sh;

   public javamon(String host, int port){
      this.host = host;
      this.port = port;
   }

   // The main method, if javamon is used as a wrapper.
   public static final void main(String[] args) throws Exception{
      int xx = 0;
      String str;
      if((str = System.getProperty("jm.port")) != null)
         try{
            xx = Integer.parseInt(str);
         }catch(Exception ex){ /* NOOP */ }
      if(xx <= 0 || xx > 65535)
         xx = 9091; // default port
      javamon jm = new javamon(((str = System.getProperty("jm.host")) != null && str.length() > 0) ? str : "127.0.0.1", xx);
      jm.start();

      // If a main class is defined, try to invoke its main() method and pass all the command line parameters, if any
      if((str = System.getProperty("jm.main")) != null && str.length() > 0){
         Class.forName(str).getMethod("main", new Class[]{String[].class}).invoke(null, new Object[]{args});
         jm.shut(); // stop javamon gracefully
      }
   }

   // Signal the HTTP interface to stop gracefully.
   public final void shut(){
      sh = true;
      try{
         ss.close();
      }catch(Exception ex){ /* NOOP */ }
   }

   // The HTTP endpoint.
   public final void run(){
      long t0 = System.currentTimeMillis();
      Socket sock;
      InputStream is;
      OutputStream os;
      Runtime run = Runtime.getRuntime();
      byte[] buf = new byte[2000];
      int rr, zz, oo, mthd, len, start, end;
      boolean http11, eol;
      while(!sh){
         try{
            (ss = new ServerSocket()).setReuseAddress(true);
            ss.bind(new InetSocketAddress(host != null ? InetAddress.getByName(host) : null, port));
            while(!sh){
               sock = null;
               try{
                  sock = ss.accept();
                  mthd = -1;
                  http11 = false;
                  len = end = 0;
                  sock.setSoTimeout(10000); // enable SO_TIMEOUT with a 10s timeout to avoid blocking indefinitely
                  sock.setTcpNoDelay(true);
                  is = sock.getInputStream();
                  os = sock.getOutputStream();
                  while(true){
                     start = end;
                     eol = false;
LN:                  while(true){
                        zz = start;
                        while(zz < len)
                           if(buf[zz++] == 10){
                              end = zz;
                              eol = true;
                              break LN;
                           }
                        if(zz >= 1024){
                           System.arraycopy(buf, start, buf, 0, len -= start);
                           start = 0;
                        }
                        try{
                           if((oo = is.read(buf, len, 2000 - len)) <= 0)
                              break;
                        }catch(SocketTimeoutException ex){
                           break;
                        }catch(InterruptedIOException ex){
                           oo = ex.bytesTransferred;
                        }
                        len += oo;
                     }
                     if(!eol){
                        try{
                           sock.setSoLinger(true, 0);
                        }catch(Exception ex){ /* NOOP */ }
                        break; // EOF / ERR
                     }
                     oo = start;
                     if(end - oo < 8){
                        if(mthd < 0)
                           continue;
                        if(mthd == 0){ // HTTP 404 Not found
                           buf[0] = 0x4E;
                           buf[1] = buf[5] = 0x6F;
                           buf[2] = 0x74;
                           buf[3] = 0x20;
                           buf[4] = 0x66;
                           buf[6] = 0x75;
                           buf[7] = 0x6E;
                           buf[8] = 0x64;
                           write(os, http11, 0x343034, buf, 9);
                           break;
                        }
                        zz = //mm(buf, // uncomment to add thread count
                                mm(buf,
                                   mm(buf,
                                      mm(buf, 0, "#TYPE heap_size_bytes gauge\nheap_size_bytes ", run.totalMemory()),
                                   "\n#TYPE heap_free_bytes gauge\nheap_free_bytes ", run.freeMemory()),
                                "\n#TYPE uptime_sec counter\nuptime_sec ", (System.currentTimeMillis() - t0) / 1000
                             //), "\n#TYPE threads gauge\nthreads ", getThreadGroup().activeCount() // uncomment to add thread count
                           );
                        buf[zz++] = buf[zz++] = 10;
                        write(os, http11, 0x323030, buf, zz); // Write the HTTP response
                        break;
                     }
                     if((buf[oo++] << 24 | buf[oo++] << 16 | buf[oo++] << 8 | buf[oo++]) != 0x47455420) // "GET "
                        continue;
                     start = rr = mthd = 0;
                     http11 = buf[end - 3] == 0x31 || buf[end - 2] == 0x31;
                     while(oo < end && (zz = buf[oo++]) != 0x20 && zz != 0x3F)
                        if(zz == 0x2F || zz == 0x5C){
                           rr = start;
                           start = 0;
                        }else
                           start = 31 * start + (zz | 0x20); // to lowercase
                     if((start == 0 ? rr : start) == 0x38F8C0C3) // metrics
                        mthd = 1;
                  }
               }catch(Exception ex){
                  if(sh)
                     return;
               }finally{
                  if(sock != null){
                     try{
                        sock.shutdownInput(); // discard any unread headers
                     }catch(Exception ex){ /* NOOP */ }
                     try{
                        sock.close();
                     }catch(Exception ex){ /* NOOP */ }
                  }
               }
            }
         }catch(Exception ex){
            System.err.print("Error starting listener. Port busy? Retrying in 10s\r\n");
         }finally{
            try{
               ss.close();
            }catch(Exception ex){ /* NOOP */ }
         }

         // Retry listening after a delay, in case the port was busy
         end = 10;
         while(!sh && end-- > 0)
            try{
               sleep(1000);
            }catch(InterruptedException ex){ /* NOOP */ }
      }
   }

   // Write the HTTP response.
   private static final void write(OutputStream os, boolean http11, int hdr, byte[] buf, int mlen) throws Exception{
      String str;
      int ii, begin, offset = begin = mlen;

      // HTTP/1.X NNN MM
      buf[offset++] = (byte)'H';
      buf[offset++] = buf[offset++] = (byte)'T';
      buf[offset++] = (byte)'P';
      buf[offset++] = (byte)'/';
      buf[offset++] = (byte)'1';
      buf[offset++] = (byte)'.';
      buf[offset++] = (byte)(http11 ? '1' : '0');
      buf[offset++] = (byte)' ';
      buf[offset++] = (byte)(hdr >> 16);
      buf[offset++] = (byte)'0';
      buf[offset++] = (byte)hdr;
      buf[offset++] = (byte)' ';
      if(hdr == 0x323030){ // "200"
         buf[offset++] = (byte)'O';
         buf[offset++] = (byte)'K';
      }else{
         buf[offset++] = (byte)'N';
         buf[offset++] = (byte)'O';
      }

      // Content-Length: NNNN
      str = "\r\nContent-Length: ";
      ii = 0;
      while(ii < 18)
         buf[offset++] = (byte)str.charAt(ii++);
      if(mlen >= 1000)
         buf[offset++] = (byte)((mlen % 10000) / 1000 + 0x30);
      if(mlen >= 100)
         buf[offset++] = (byte)((mlen % 1000) / 100 + 0x30);
      if(mlen >= 10)
         buf[offset++] = (byte)((mlen % 100) / 10 + 0x30);
      buf[offset++] = (byte)(mlen % 10 + 0x30);
      buf[offset++] = (byte)'\r';
      buf[offset++] = (byte)'\n';

      // Connection: close (for HTTP 1.1 only)
      if(http11){
         str = "Connection: close\r\n";
         ii = 0;
         while(ii < 19)
            buf[offset++] = (byte)str.charAt(ii++);
      }
      buf[offset++] = (byte)'\r';
      buf[offset++] = (byte)'\n';
      System.arraycopy(buf, 0, buf, offset, mlen);
      ii = 0;
      while(true)
         try{
            os.write(buf, begin + ii, offset + mlen - begin - ii);
            break;
         }catch(InterruptedIOException ex){
            ii += ex.bytesTransferred;
         }
   }

   // Add a metric to the current report.
   private static final int mm(byte[] buf, int offset, String str, long ii){
      long jj = ii & 0xFFFFFFFFFFL; // ~1 Tb
      int llen = jj <= 9 ? 1 : jj <= 99 ? 2 : jj <= 999 ? 3 : jj <= 9999 ? 4 : jj <= 99999 ? 5 : jj <= 999999 ? 6 : jj <= 9999999 ? 7 : jj <= 99999999 ? 8 : jj <= 999999999 ? 9 : jj <= 9999999999L ? 10 : jj <= 99999999999L ? 11 : jj <= 999999999999L ? 12 : 13;
      int slen = str.length(), newCount = offset + slen + llen, xx = 0;
      while(xx < slen)
         buf[offset++] = (byte)str.charAt(xx++);
      xx = newCount;
      while(llen-- > 0){
         buf[--newCount] = (byte)(0x30 + (int)(jj % 10));
         jj /= 10;
      }
      return xx;
   }
}
