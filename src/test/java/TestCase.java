/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.util.concurrent.CountDownLatch;

import vavi.sound.shazam.view.MainView;

import org.junit.jupiter.api.Test;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-01-12 nsano initial version <br>
 */
public class TestCase {

    @Test
    void test1() throws Exception {
        MainView.main(new String[] {});

        CountDownLatch cld = new CountDownLatch(1);
        cld.await();
    }
}
