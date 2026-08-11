package org.thuantn.desktopsearch.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    
    private static final Properties PROPERTIES = new Properties();
    
    static {
        
        try (
            InputStream input =
                AppConfig.class
                    .getClassLoader()
                    .getResourceAsStream("application.properties")) {
            
            if (input == null) {
                throw new IllegalStateException(
                    "application.properties not found"
                );
            }
            
            PROPERTIES.load(input);
            
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    private AppConfig() {
    
    }
    
    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }
    
}
