// Permission is granted, free of charge, to any person obtaining a copy of this software and associated
// documentation, to deal in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// to permit persons to whom the Software is furnished to do so, subject to the condition that this
// copyright shall be included in all copies or substantial portions of the Software:
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

// The Java monitoring agent exposes an HTTP endpoint compatible with Prometheus.
public final class javamon extends Thread{

   private final String host; // HTTP listener host or IP address
   private ServerSocket ss;
   private final int port;    // HTTP listener port
   private boolean sh;        // Signal the HTTP listener to stop

   public javamon(String host, int port){
      this.host = host;
      this.port = port;
   }

   // The main method, if javamon is used as a wrapper (launcher).
   public static final void main(String[] args) throws Exception{
      int port = 0;
      String str;
      if((str = System.getProperty("jm.port")) != null)
         try{
            port = Integer.parseInt(str);
         }catch(Exception ex){ /* NOOP */ }
      if(port <= 0 || port > 65535)
         port = 9091; // default port
      javamon jm = new javamon((str = System.getProperty("jm.host")) != null && str.length() > 0 ? str : "127.0.0.1", port);
      jm.start();

      // If a main class is defined, try to invoke its main() method and pass all the command line parameters, if any
      if((str = System.getProperty("jm.main")) != null && str.length() > 0){
         Class.forName(str).getMethod("main", new Class[]{ Class.forName("[Ljava.lang.String;") }).invoke(null, new Object[]{ args });
         jm.shut(); // stop javamon gracefully before exiting
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
      long t0 = System.currentTimeMillis(); // the starting time (t0) to compute the uptime
      Socket sock;
      byte[] buf = new byte[2000];
      Runtime run = Runtime.getRuntime();
      int rr, zz, oo, mthd, len, start, end;
      boolean http11;
      while(!sh){
         try{
            (ss = new ServerSocket()).setReuseAddress(true);
            ss.bind(new InetSocketAddress(host != null ? InetAddress.getByName(host) : null, port));
            while(!sh){
               http11 = false;
               len = end = 0;
               mthd = -1;
               sock = null;
               try{
                  (sock = ss.accept()).setSoTimeout(10000); // enable SO_TIMEOUT with a 10s timeout to avoid blocking indefinitely
                  sock.setTcpNoDelay(true);
                  InputStream is = sock.getInputStream();
SRV:              while(true){
                     start = end;
LN:                  {
                        while(true){
                           zz = start;
                           while(zz < len)
                              if(buf[zz++] == 10){
                                 end = zz;
                                 break LN;
                              }
                           if(zz >= 1000){
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
                        try{
                           sock.setSoLinger(true, 0);
                        }catch(Exception ex){ /* NOOP */ }
                        break; // EOF / ERR
                     }
                     oo = start;
                     if(end - oo < 8){
                        if(mthd < 0)
                           continue;
                        if(mthd == 0){
                           mthd = 0x343034; // HTTP 404 Not found
                           buf[0] = (byte)'N';
                           buf[1] = buf[5] = (byte)'o';
                           buf[2] = (byte)'t';
                           buf[3] = (byte)' ';
                           buf[4] = (byte)'f';
                           buf[6] = (byte)'u';
                           buf[7] = (byte)'n';
                           buf[8] = (byte)'d';
                           zz = 9;
                        }else{
                           mthd = 0x323030; // HTTP 200 OK
                           zz = //mm(buf, // uncomment to add thread count
                              mm(buf,
                                 mm(buf,
                                    mm(buf, 0, "#TYPE heap_size_bytes gauge\nheap_size_bytes ", 44, run.totalMemory()),
                                 "\n#TYPE heap_free_bytes gauge\nheap_free_bytes ", 45, run.freeMemory()),
                              "\n#TYPE uptime_sec counter\nuptime_sec ", 37, (System.currentTimeMillis() - t0) / 1000
                           //), "\n#TYPE threads gauge\nthreads ", 29, getThreadGroup().activeCount() // uncomment to add thread count
                           );
                           buf[zz++] = buf[zz++] = 10;
                        }

                        // HTTP/1.X NNN MM
                        rr = start = zz;
                        buf[rr++] = (byte)'H';
                        buf[rr++] = buf[rr++] = (byte)'T';
                        buf[rr++] = (byte)'P';
                        buf[rr++] = (byte)'/';
                        buf[rr++] = (byte)'1';
                        buf[rr++] = (byte)'.';
                        buf[rr++] = (byte)(http11 ? '1' : '0');
                        buf[rr++] = (byte)' ';
                        buf[rr++] = (byte)(mthd >> 16);
                        buf[rr++] = (byte)'0';
                        buf[rr++] = (byte)mthd;
                        buf[rr++] = (byte)' ';
                        if(mthd == 0x323030){ // "200"
                           buf[rr++] = (byte)'O';
                           buf[rr++] = (byte)'K';
                        }else{
                           buf[rr++] = (byte)'N';
                           buf[rr++] = (byte)'O';
                        }

                        // Content-Length: NNNN
                        String str = "\r\nContent-Length: ";
                        oo = 0;
                        while(oo < 18)
                           buf[rr++] = (byte)str.charAt(oo++);
                        if(zz >= 1000)
                           buf[rr++] = (byte)((zz % 10000) / 1000 + 0x30);
                        if(zz >= 100)
                           buf[rr++] = (byte)((zz % 1000) / 100 + 0x30);
                        if(zz >= 10)
                           buf[rr++] = (byte)((zz % 100) / 10 + 0x30);
                        buf[rr++] = (byte)(zz % 10 + 0x30);
                        buf[rr++] = (byte)'\r';
                        buf[rr++] = (byte)'\n';

                        // Connection: close (for HTTP 1.1 only)
                        if(http11){
                           str = "Connection: close\r\n";
                           oo = 0;
                           while(oo < 19)
                              buf[rr++] = (byte)str.charAt(oo++);
                        }
                        buf[rr++] = (byte)'\r';
                        buf[rr++] = (byte)'\n';
                        System.arraycopy(buf, 0, buf, rr, zz);
                        OutputStream os = sock.getOutputStream();
                        while(true)
                           try{
                              os.write(buf, start, rr + zz - start);
                              break SRV;
                           }catch(InterruptedIOException ex){
                              start += ex.bytesTransferred;
                           }
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
            System.err.print("Error listening. Port busy? Retrying\r\n");
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

   // Add a metric to the current report.
   private static final int mm(byte[] buf, int off, String str, int slen, long ii){
      ii &= 0xFFFFFFFFFFL; // ~1 Tb
      int xx = 0, llen = ii <= 9 ? 1 : ii <= 99 ? 2 : ii <= 999 ? 3 : ii <= 9999 ? 4 : ii <= 99999 ? 5 : ii <= 999999 ? 6 : ii <= 9999999 ? 7 : ii <= 99999999 ? 8 : ii <= 999999999 ? 9 : ii <= 9999999999L ? 10 : ii <= 99999999999L ? 11 : ii <= 999999999999L ? 12 : 13;
      while(xx < slen)
         buf[off++] = (byte)str.charAt(xx++);
      xx = off += llen;
      while(llen-- > 0){
         buf[--off] = (byte)(0x30 + (int)(ii % 10));
         ii /= 10;
      }
      return xx;
   }
}
