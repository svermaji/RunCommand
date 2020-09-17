package com.sv.runcmd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class DefaultConfigs {

    private URL propUrl;
    enum Config {
        RANDOM_THEMES("RandomThemes"),
        RANDOM_COLORS("RandomColors");

        String val;
        Config (String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }
    }

    String propFileName = "./conf.config";
    private final Properties configs = new Properties();
    private MyLogger logger;

    public DefaultConfigs(MyLogger logger) {
        this.logger = logger;
        initialize();
    }

    public void initialize() {
        readConfig();
    }

    public String getConfig(Config config) {
        if (configs.containsKey(config.getVal()))
            return configs.getProperty(config.getVal());
        return Utils.EMPTY;
    }

    private void readConfig() {
        logger.log ("Loading properties from path: " + propFileName);
        try (InputStream is = Files.newInputStream(Paths.get(propFileName))) {
            propUrl = Paths.get(propFileName).toUri().toURL();
            configs.load(is);
        } catch (Exception e) {
            logger.log ("Error in loading properties via file path, trying class loader.");
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(propFileName)) {
                propUrl = Paths.get(propFileName).toUri().toURL();
                configs.load(is);
            } catch (IOException ioException) {
                logger.log ("Error in loading properties via class loader.");
            }
        }
        logger.log ("Prop url calculated as: " + propUrl);
    }

    public void saveConfig(Object obj) {
        logger.log ("Saving properties at " + propUrl.getPath());
        configs.clear();
        for (Config config : Config.values()) {
            try {
                configs.put(config.getVal(), obj.getClass().getDeclaredMethod("get" + config.getVal()).invoke(obj));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                logger.log ("Error in calling method: get " + config.getVal());
            }
        }
        logger.log ("Config is " + configs);
        try {
            configs.store(new FileOutputStream(propUrl.getPath()), null);
        } catch (IOException e) {
            logger.log ("Error in saving properties.");
        }
    }

}
