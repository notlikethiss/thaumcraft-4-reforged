package thaumcraft.entity.golem;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import thaumcraft.entity.golem.goal.work.CreeperSwellAvoidGoal;
import thaumcraft.entity.golem.goal.work.CropHarvestGoal;
import thaumcraft.entity.golem.goal.work.FishingGoal;
import thaumcraft.entity.golem.goal.work.GolemCombatGoals;
import thaumcraft.entity.golem.goal.work.LogHarvestGoal;
import thaumcraft.entity.golem.goal.work.UseItemGoal;

public final class WorkGoals {
    private WorkGoals() {
    }

    public static void install(GolemBase golem, GoalSelector goals, GoalSelector targets) {
        if (golem.getCore() < 0) return;
        goals.addGoal(0, new CreeperSwellAvoidGoal(golem));
        switch (golem.getCore()) {
            case 3 -> goals.addGoal(2, new CropHarvestGoal(golem));
            case 4 -> GolemCombatGoals.install(golem, goals, targets, false);
            case 7 -> goals.addGoal(2, new LogHarvestGoal(golem));
            case 8 -> goals.addGoal(0, new UseItemGoal(golem));
            case 9 -> GolemCombatGoals.install(golem, goals, targets, true);
            case 11 -> goals.addGoal(2, new FishingGoal(golem));
            default -> {
            }
        }
    }
}
