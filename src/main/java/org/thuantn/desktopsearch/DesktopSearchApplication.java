package org.thuantn.desktopsearch;

import org.thuantn.desktopsearch.scanner.FileScanner;
import org.thuantn.desktopsearch.scanner.NioFileScanner;

import java.nio.file.Path;

public class DesktopSearchApplication {

    public static void main(String[] args) throws Exception {

        FileScanner scanner = new NioFileScanner();

        scanner.scan(
                Path.of("/home/thuantn/cobltool/docs"),
                System.out::println
        );
    }

}
