package thaumcraft.network;

import thaumcraft.blockentity.ResearchTableBlockEntity;
import thaumcraft.platform.ModNetwork;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ScanManager;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register() {
        ModNetwork.toClient(BoreDigPayload.TYPE, BoreDigPayload.STREAM_CODEC);
        ModNetwork.toClient(BeamPulsePayload.TYPE, BeamPulsePayload.STREAM_CODEC);
        ModNetwork.toClient(AspectTagsPayload.TYPE, AspectTagsPayload.STREAM_CODEC);
        ModNetwork.toClient(NodeZapPayload.TYPE, NodeZapPayload.STREAM_CODEC);
        ModNetwork.toClient(VisDrainFxPayload.TYPE, VisDrainFxPayload.STREAM_CODEC);
        ModNetwork.toClient(ResearchCompletePayload.TYPE, ResearchCompletePayload.STREAM_CODEC);
        ModNetwork.toClient(AspectPoolPayload.TYPE, AspectPoolPayload.STREAM_CODEC);
        ModNetwork.toClient(AspectDiscoveryPayload.TYPE, AspectDiscoveryPayload.STREAM_CODEC);
        ModNetwork.toClient(WarpMessagePayload.TYPE, WarpMessagePayload.STREAM_CODEC);
        ModNetwork.toClient(WarpEventPayload.TYPE, WarpEventPayload.STREAM_CODEC);
        ModNetwork.toClient(EssentiaSourceFxPayload.TYPE, EssentiaSourceFxPayload.STREAM_CODEC);
        ModNetwork.toClient(InfusionSourceFxPayload.TYPE, InfusionSourceFxPayload.STREAM_CODEC);
        ModNetwork.toClient(BlockSparkleFxPayload.TYPE, BlockSparkleFxPayload.STREAM_CODEC);
        ModNetwork.toServer(ScanPayload.TYPE, ScanPayload.STREAM_CODEC, ScanManager::handleScan);
        ModNetwork.toServer(PlayerCompletePayload.TYPE, PlayerCompletePayload.STREAM_CODEC, ResearchManager::handlePlayerComplete);
        ModNetwork.toServer(AspectPlacePayload.TYPE, AspectPlacePayload.STREAM_CODEC, ResearchTableBlockEntity::handlePlace);
        ModNetwork.toServer(AspectCombinePayload.TYPE, AspectCombinePayload.STREAM_CODEC, ResearchTableBlockEntity::handleCombine);
        ModNetwork.toServer(HoverTogglePayload.TYPE, HoverTogglePayload.STREAM_CODEC, HoverTogglePayload::handle);
        ModNetwork.toClient(ShockZapPayload.TYPE, ShockZapPayload.STREAM_CODEC);
        ModNetwork.toClient(RunicChargePayload.TYPE, RunicChargePayload.STREAM_CODEC);
        ModNetwork.toClient(ShieldFxPayload.TYPE, ShieldFxPayload.STREAM_CODEC);
        ModNetwork.toServer(FocusChangePayload.TYPE, FocusChangePayload.STREAM_CODEC, FocusChangePayload::handle);
        ModNetwork.toServer(MiscTogglePayload.TYPE, MiscTogglePayload.STREAM_CODEC, MiscTogglePayload::handle);
        ModNetwork.toServer(BellResetPayload.TYPE, BellResetPayload.STREAM_CODEC, BellResetPayload::handle);
        ModNetwork.toClient(BlockBubbleFxPayload.TYPE, BlockBubbleFxPayload.STREAM_CODEC);
    }
}
