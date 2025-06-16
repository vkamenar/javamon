// This sample application doesn't use the javamon API.
// Therefore, it can be used to test javamon as a wrapper.
public class TestWrapper{

   // Print some text to the console and wait for user input.
   public static void main(String[] args) throws java.io.IOException{
      System.in.skip(System.in.available()); // discard any buffered input
      if(args != null)
        for(int x = 0; x < args.length; x++)
           System.out.println("Command-line argument: " + args[x]);
      System.out.println("Press Enter to end this program");
      System.in.read();
   }
}