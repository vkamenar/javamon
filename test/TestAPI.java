import com.agent.javamon;

// This sample application uses the javamon API.
public class TestAPI{

   // Print some text to the console and wait for user input.
   public static void main(String[] args) throws java.io.IOException{
      System.in.skip(System.in.available()); // discard any buffered input
      System.out.println("Monitoring endpoint: http://127.0.0.1:9091/metrics");
      javamon jm = new javamon("127.0.0.1", 9091);
      jm.start();

      System.out.println("Press Enter to end this program");
      System.in.read();
      jm.shut(); // stop the monitoring endpoint gracefully
   }
}
