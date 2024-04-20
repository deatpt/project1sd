package utils;

import java.util.logging.Level;
import java.util.logging.LogManager;

public class Debug {

    public static void setLogLevel(Level newLevel) {
        var rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(newLevel);
        for (var h : rootLogger.getHandlers()) 
            h.setLevel( newLevel );
    }

}