package cucumber.runner;

import com.qunhe.avocado.lib.constant.Settings;
import cucumber.api.Result;
import cucumber.api.TestStep;
import cucumber.api.event.Event;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestCaseStarted;
import gherkin.events.PickleEvent;
import gherkin.pickles.PickleLocation;
import gherkin.pickles.PickleTag;

import java.util.ArrayList;
import java.util.List;

final class TestCase implements cucumber.api.TestCase {
    private final PickleEvent pickleEvent;
    private final List<PickleStepTestStep> testSteps;
    private final boolean dryRun;
    private final List<HookTestStep> beforeHooks;
    private final List<HookTestStep> afterHooks;

    public TestCase(List<PickleStepTestStep> testSteps,
                    List<HookTestStep> beforeHooks,
                    List<HookTestStep> afterHooks,
                    PickleEvent pickleEvent,
                    boolean dryRun) {
        this.testSteps = testSteps;
        this.beforeHooks = beforeHooks;
        this.afterHooks = afterHooks;
        this.pickleEvent = pickleEvent;
        this.dryRun = dryRun;
    }

    // dongqing revise
    void run(EventBus bus) {
        Long startTime = bus.getTime();
        bus.send(new TestCaseStarted(startTime, this));
        int failTryCount = Settings.FAIL_TRY_TIMES;
        Scenario scenario = new Scenario(bus, this);

        do {
            scenario.clearResults();
            failTryCount--;
            scenario.setRunCount(failTryCount);
            realRun(scenario, bus);
        } while (scenario.getError() != null && failTryCount > 0);

        Long stopTime = bus.getTime();
        bus.send(new TestCaseFinished(stopTime, this, new Result(scenario.getStatus(),
            stopTime - startTime, scenario.getError())));
    }

    void realRun(Scenario scenario, EventBus bus) {
        boolean skipNextStep = this.dryRun;

        for (HookTestStep before : beforeHooks) {
            skipNextStep |= before.run(this, bus, scenario, dryRun);
        }

        for (PickleStepTestStep step : testSteps) {
            if (skipNextStep && scenario.getRunCount() > 0) {
                break;
            }
            skipNextStep |= step.run(this, bus, scenario, skipNextStep);
        }

        for (HookTestStep after : afterHooks) {
            after.run(this, bus, scenario, dryRun);
        }
    }

    @Override
    public List<TestStep> getTestSteps() {
        List<TestStep> testSteps = new ArrayList<TestStep>(beforeHooks);
        for (PickleStepTestStep step : this.testSteps) {
            testSteps.addAll(step.getBeforeStepHookSteps());
            testSteps.add(step);
            testSteps.addAll(step.getAfterStepHookSteps());
        }
        testSteps.addAll(afterHooks);
        return testSteps;
    }

    @Override
    public String getName() {
        return pickleEvent.pickle.getName();
    }

    @Override
    public String getScenarioDesignation() {
        return fileColonLine(pickleEvent.pickle.getLocations().get(0)) + " # " + getName();
    }

    @Override
    public String getUri() {
        return pickleEvent.uri;
    }

    @Override
    public int getLine() {
        return pickleEvent.pickle.getLocations().get(0).getLine();
    }

    public List<Integer> getLines() {
        List<Integer> lines = new ArrayList<>();
        for (PickleLocation location : pickleEvent.pickle.getLocations()) {
            lines.add(location.getLine());
        }
        return lines;
    }

    private String fileColonLine(PickleLocation location) {
        return pickleEvent.uri + ":" + location.getLine();
    }

    @Override
    public List<PickleTag> getTags() {
        return pickleEvent.pickle.getTags();
    }
}
