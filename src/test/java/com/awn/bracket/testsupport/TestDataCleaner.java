package com.awn.bracket.testsupport;

import com.awn.bracket.shared.utils.DataCleaning;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;

public class TestDataCleaner implements QuarkusTestAfterAllCallback {

    @Override
    public void afterAll(QuarkusTestContext context) {
        ArcContainer container = Arc.container();
        if (container == null) {
            return;
        }
        InstanceHandle<DataCleaning> handle = container.instance(DataCleaning.class);
        if (handle != null && handle.get() != null) {
            handle.get().cleanTestDatabase();
        }
    }
}
