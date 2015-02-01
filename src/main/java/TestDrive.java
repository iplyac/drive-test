import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;

import java.io.IOException;

public class TestDrive {

    public static void main(String[] args)
    {
        try {
            GoogleAuthorizationCodeFlow flow = Auth.getFlow();

            System.out.println(flow.getClientId());
        }catch (IOException e)
        {
            //TODO:
        }

    }
}
