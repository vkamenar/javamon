package com.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

// The Java monitoring agent
public final class javamon extends Thread implements CharSequence{

   private final byte[] b;
   private char[] cvalue;
   private final String host;
   private ServerSocket ss;
   private final int port;
   private int len, start, end, count;
   private boolean sh;

   public javamon(String host, int port){
      this.host = host;
      this.port = port;
      b = new byte[0x1000];
      cvalue = new char[180];
   }

   // The main method, if javamon is used as a java agent
   public static final void main(String[] args) throws Exception{

      // Parse the configuration parameters: host, port and main class
      int xx = 0;
      String str, host = "127.0.0.1", main = null;
      if((str = System.getProperty("jm.host")) != null && str.length() > 0)
         host = str;
      if((str = System.getProperty("jm.port")) != null)
         try{
            xx = Integer.parseInt(str);
         }catch(Exception ex){ /* NOOP */ }
      if(xx <= 0 || xx > 65535)
         xx = 9091;
      if((str = System.getProperty("jm.main")) != null && str.length() > 0)
         main = str;
      javamon jm = new javamon(host, xx);
      jm.start();

      // If a main class is defined, try to invoke its main() method and
      // pass all the command line parameters, if any
      if(main != null){
         Class.forName(main).getMethod("main", new Class[]{String[].class}).invoke(null, new Object[]{args});
         jm.shutdown(); // stop the javamon gracefully
      }
   }

   // Signal the HTTP interface to stop gracefully
   public final void shutdown(){
      sh = true;
      try{
         ss.close();
      }catch(Exception ex){ /* NOOP */ }
   }

   // The HTTP endpoint
   public final void run(){
      long t0 = System.currentTimeMillis();
      Socket sock;
      InputStream is;
      OutputStream os;
      Runtime run = Runtime.getRuntime();
      byte[] buf = b;
      int ii, rr, zz, oo, hdr, mthd;
      boolean http11, eol;
      while(!sh){
         try{
            (ss = new ServerSocket()).setReuseAddress(true);
            InetAddress iaddr = null;
            if(host != null)
               iaddr = InetAddress.getByName(host);
            ss.bind(new InetSocketAddress(iaddr, port));
            while(!sh){
               sock = null;
               try{
                  sock = ss.accept();
                  mthd = -1;
                  http11 = false;
                  len = ii = 0;
                  sock.setSoTimeout(10000);
                  sock.setTcpNoDelay(true);
                  is = sock.getInputStream();
                  os = sock.getOutputStream();
                  while(true){
                     start = ii;
                     eol = false;
LN:                  while(true){
                        zz = start;
                        while(zz < len)
                           if(buf[zz++] == 10){
                              end = zz;
                              eol = true;
                              break LN;
                           }
                        if(zz >= 2048){
                           System.arraycopy(buf, start, buf, 0, len -= start);
                           start = 0;
                        }
                        try{
                           if((oo = is.read(buf, len, 2048 - len)) <= 0)
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
                     ii = end;
                     oo = start;
                     if(ii - oo < 8){
                        if(mthd < 0)
                           continue;
                        if(mthd == 0){
                           write(os, http11, 0x343034, "Not found", 9);
                           break;
                        }
                        hdr = 0x323030; // HTTP 200
                        count = 0;
                        mm("#TYPE heap_size_bytes gauge\nheap_size_bytes ", run.totalMemory());
                        mm("\n#TYPE heap_free_bytes gauge\nheap_free_bytes ", run.freeMemory());
                        mm("\n#TYPE uptime_ms counter\nuptime_ms ", System.currentTimeMillis() - t0);
                        zz = count;
                        cvalue[zz++] = cvalue[zz++] = '\n';
                        write(os, http11, hdr, this, zz); // Write the HTTP response
                        break;
                     }
                     if((buf[oo++] << 24 | buf[oo++] << 16 | buf[oo++] << 8 | buf[oo++]) != 0x47455420) // "GET "
                        continue;
                     hdr = rr = mthd = 0;
                     http11 = buf[ii - 3] == 0x31 || buf[ii - 2] == 0x31;
                     while(oo < ii && (zz = buf[oo++]) != 0x20 && zz != 0x3F)
                        if(zz == 0x2F || zz == 0x5C){
                           rr = hdr;
                           hdr = 0;
                        }else
                           hdr = 31 * hdr + (zz | 0x20); // to lowercase
                     if(hdr == 0)
                        hdr = rr;
                     if(hdr == 0x38F8C0C3) // metrics
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
         }catch(Exception ex){ /* NOOP */
         }finally{
            try{
               ss.close();
            }catch(Exception ex){ /* NOOP */ }
         }

         // Retry listening after a delay, in case the port was busy
         ii = 10;
         while(ii-- > 0){
            if(sh)
               return;
            try{
               sleep(1000);
            }catch(InterruptedException ex){ /* NOOP */ }
         }
      }
   }

   // Write the HTTP response
   private final void write(OutputStream os, boolean http11, int hdr, CharSequence msg, int mlen) throws Exception{
      String str;
      byte[] buf = b;
      int ii, begin = len, offset = begin;

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
      buf[offset++] = (byte)(hdr >> 8);
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
      ii = 0;
      while(ii < mlen)
         buf[offset++] = (byte)msg.charAt(ii++);
      ii = 0;
      while(true)
         try{
            os.write(buf, begin + ii, offset - begin - ii);
            break;
         }catch(InterruptedIOException ex){
            ii += ex.bytesTransferred;
         }
   }

   // Add a metric to the current report
   private final void mm(String str, long ii){
      long jj = ii & 0xFFFFFFFFFFL; // ~1 Tb / ~30 years
      int llen = jj <= 9 ? 1 : jj <= 99 ? 2 : jj <= 999 ? 3 : jj <= 9999 ? 4 : jj <= 99999 ? 5 : jj <= 999999 ? 6 : jj <= 9999999 ? 7 : jj <= 99999999 ? 8 : jj <= 999999999 ? 9 : jj <= 9999999999L ? 10 : jj <= 99999999999L ? 11 : jj <= 999999999999L ? 12 : 13;
      int slen = str.length(), newCount = count + slen + llen, newSize = newCount + 2;
      if(newSize > cvalue.length){
         int newcapacity = (cvalue.length + 1) << 1;
         if(newSize > newcapacity)
            newcapacity = newSize;
         char[] newval = new char[newcapacity];
         System.arraycopy(cvalue, 0, newval, 0, count);
         cvalue = newval;
      }
      char[] lval = cvalue;
      str.getChars(0, slen, lval, count);
      count = newCount;
      while(llen-- > 0){
         lval[--newCount] = (char)(0x30 + (int)(jj % 10));
         jj /= 10;
      }
   }

   // Returns the char value at the specified index
   public final char charAt(int index){
      return cvalue[index];
   }

   // Create a subsequence of this sequence, defined in the CharSequence interface (not used in javamon)
   public final CharSequence subSequence(int start, int end){
      return null;
   }

   // The length of the character sequence, defined in the CharSequence interface (not used in javamon)
   public final int length(){
      return 0;
   }
}
