package com.practice.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.practice.visual.dao.DashboardScreenDao;
import com.practice.visual.entity.DashboardScreen;
import com.practice.visual.init.DataInitializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DataInitializerTest {
    @Test
    void initializesOnlyDashboardScreenWhenEmpty() throws Exception {
        DashboardScreenDao screens = mock(DashboardScreenDao.class);
        when(screens.count()).thenReturn(0L);
        DataInitializer initializer = new DataInitializer(screens);

        initializer.run();

        ArgumentCaptor<DashboardScreen> captor = ArgumentCaptor.forClass(DashboardScreen.class);
        verify(screens).save(captor.capture());
        assertEquals("screen-factory", captor.getValue().code);
        assertEquals("PUBLISHED", captor.getValue().status);
    }

    @Test
    void doesNotDuplicateExistingDashboardScreen() throws Exception {
        DashboardScreenDao screens = mock(DashboardScreenDao.class);
        when(screens.count()).thenReturn(1L);
        DataInitializer initializer = new DataInitializer(screens);

        initializer.run();

        verify(screens, never()).save(any(DashboardScreen.class));
    }
}
