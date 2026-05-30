package dev.vethcraft.vethenchant.util;

public record CustomCropData(
    String cropId,
    String shortCropId,
    String seedId,
    String firstStageId,
    String matureStageId,
    int firstStage,
    int matureStage
) {

    public boolean isMatureStage(String stageId) {
        return this.matureStageId.equals(stageId);
    }
}
