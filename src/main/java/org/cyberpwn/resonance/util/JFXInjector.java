package org.cyberpwn.resonance.util;

import org.cyberpwn.resonance.player.FilePlayer;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;


public class JFXInjector {
public static ClassLoader loader;

    public static void inject() {
        File jfx = new File("lib/jfxrt.jar");

        if(!jfx.exists())
        {
            System.out.println("Installing jfxrt");
            jfx.getParentFile().mkdirs();
            InputStream in = JFXInjector.class.getResourceAsStream("/jfxrt.jar");
            try {
                Files.copy(in, jfx.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try
        {
            System.out.println("Injecting jfxrt");
            URLClassLoader child = new URLClassLoader(
                    new URL[] {jfx.toURI().toURL()},
                    JFXInjector.class.getClassLoader());
            loader = child;

            final CountDownLatch latch = new CountDownLatch(1);

            SwingUtilities.invokeLater(() -> {
                try {
                    System.out.println("Starting jfxrt");
                    child.loadClass("javafx.embed.swing.JFXPanel").getConstructor().newInstance();
                    child.loadClass("javafx.scene.media.MediaPlayer");
                    child.loadClass("org.cyberpwn.resonance.player.FilePlayer");


                } catch (Throwable e) {
                    e.printStackTrace();
                }
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            System.out.println("Jfxrt ready!");
        }

        catch(Throwable e)
        {
            e.printStackTrace();
        }
    }
}
