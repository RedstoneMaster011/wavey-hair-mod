package dev.redstone.hairphysics.client.data;

import java.util.Collections;
import java.util.List;































public class HairDefinition {

    public enum PlayerModel {
        CLASSIC, SLIM
    }

    public final int version;
    public final PlayerModel model;
    public final List<HairStrand> strands;

    public HairDefinition(int version, String model, List<HairStrand> strands) {
        this.version = version;
        this.model = parseModel(model);
        this.strands = strands != null ? Collections.unmodifiableList(strands) : List.of();
    }

    private static PlayerModel parseModel(String s) {
        if ("slim".equalsIgnoreCase(s)) return PlayerModel.SLIM;
        return PlayerModel.CLASSIC;
    }

    public boolean isSlim() {
        return model == PlayerModel.SLIM;
    }

    public boolean hasStrands() {
        return !strands.isEmpty();
    }

    @Override
    public String toString() {
        return "HairDefinition{version=" + version + ", model=" + model +
               ", strands=" + strands.size() + "}";
    }
}
