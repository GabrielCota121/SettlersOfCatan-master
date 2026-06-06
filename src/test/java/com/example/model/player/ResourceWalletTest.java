package com.example.model.player;

import com.example.model.game.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceWalletTest {

    private ResourceWallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new ResourceWallet();
    }

    @Test
    void initialWallet_allResourcesAreZero() {
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                assertEquals(0, wallet.getResourceAmount(type));
            }
        }
    }

    @Test
    void initialTotalCards_isZero() {
        assertEquals(0, wallet.getTotalCards());
    }

    @Test
    void initiallyNotHidden() {
        assertFalse(wallet.isHidden());
    }

    @Test
    void addResource_increasesAmount() {
        wallet.addResource(ResourceType.WOOD, 3);
        assertEquals(3, wallet.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void addResource_updatesTotal() {
        wallet.addResource(ResourceType.ORE, 2);
        wallet.addResource(ResourceType.WHEAT, 1);
        assertEquals(3, wallet.getTotalCards());
    }

    @Test
    void addResource_notifiesListener() {
        int[] count = {0};
        wallet.setOnWalletChangedListener(() -> count[0]++);
        wallet.addResource(ResourceType.BRICK, 1);
        assertEquals(1, count[0]);
    }

    @Test
    void addResource_multipleTypes_sumsCorrectly() {
        wallet.addResource(ResourceType.WOOD, 2);
        wallet.addResource(ResourceType.BRICK, 3);
        wallet.addResource(ResourceType.WOOL, 1);
        assertEquals(6, wallet.getTotalCards());
    }

    @Test
    void setResource_setsAbsoluteValue() {
        wallet.addResource(ResourceType.WOOL, 5);
        wallet.setResource(ResourceType.WOOL, 2);
        assertEquals(2, wallet.getResourceAmount(ResourceType.WOOL));
    }

    @Test
    void setResource_clampsNegativeToZero() {
        wallet.setResource(ResourceType.WOOD, -3);
        assertEquals(0, wallet.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void setResource_ignoresDesert() {
        // DESERT is not stored in wallet — should not throw
        wallet.setResource(ResourceType.DESERT, 5);
        assertEquals(0, wallet.getTotalCards());
    }

    @Test
    void setResource_notifiesListener() {
        int[] count = {0};
        wallet.setOnWalletChangedListener(() -> count[0]++);
        wallet.setResource(ResourceType.ORE, 3);
        assertEquals(1, count[0]);
    }

    @Test
    void removeResource_decreasesAmount() {
        wallet.addResource(ResourceType.ORE, 5);
        assertTrue(wallet.removeResource(ResourceType.ORE, 3));
        assertEquals(2, wallet.getResourceAmount(ResourceType.ORE));
    }

    @Test
    void removeResource_exactAmount_returnsTrue() {
        wallet.addResource(ResourceType.WHEAT, 4);
        assertTrue(wallet.removeResource(ResourceType.WHEAT, 4));
        assertEquals(0, wallet.getResourceAmount(ResourceType.WHEAT));
    }

    @Test
    void removeResource_insufficient_returnsFalse() {
        wallet.addResource(ResourceType.WOOD, 2);
        assertFalse(wallet.removeResource(ResourceType.WOOD, 3));
        assertEquals(2, wallet.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void removeResource_insufficient_doesNotChangeAmount() {
        wallet.addResource(ResourceType.WOOD, 2);
        wallet.removeResource(ResourceType.WOOD, 5);
        assertEquals(2, wallet.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void removeResource_insufficient_doesNotNotifyListener() {
        wallet.addResource(ResourceType.WOOD, 2);
        int[] count = {0};
        wallet.setOnWalletChangedListener(() -> count[0]++);
        wallet.removeResource(ResourceType.WOOD, 5);
        assertEquals(0, count[0]);
    }

    @Test
    void removeResource_success_notifiesListener() {
        wallet.addResource(ResourceType.WOOD, 3);
        int[] count = {0};
        wallet.setOnWalletChangedListener(() -> count[0]++);
        wallet.removeResource(ResourceType.WOOD, 1);
        assertEquals(1, count[0]);
    }

    @Test
    void hasEnoughResources_byParams_allEnough_returnsTrue() {
        wallet.addResource(ResourceType.WOOD, 2);
        wallet.addResource(ResourceType.BRICK, 1);
        assertTrue(wallet.hasEnoughResources(2, 1, 0, 0, 0));
    }

    @Test
    void hasEnoughResources_byParams_woodInsufficient_returnsFalse() {
        wallet.addResource(ResourceType.WOOD, 1);
        assertFalse(wallet.hasEnoughResources(2, 0, 0, 0, 0));
    }

    @Test
    void hasEnoughResources_byParams_brickInsufficient_returnsFalse() {
        assertFalse(wallet.hasEnoughResources(0, 1, 0, 0, 0));
    }

    @Test
    void hasEnoughResources_byParams_woolInsufficient_returnsFalse() {
        assertFalse(wallet.hasEnoughResources(0, 0, 1, 0, 0));
    }

    @Test
    void hasEnoughResources_byParams_wheatInsufficient_returnsFalse() {
        assertFalse(wallet.hasEnoughResources(0, 0, 0, 1, 0));
    }

    @Test
    void hasEnoughResources_byParams_oreInsufficient_returnsFalse() {
        assertFalse(wallet.hasEnoughResources(0, 0, 0, 0, 1));
    }

    @Test
    void hasEnoughResources_byMap_allEnough_returnsTrue() {
        wallet.addResource(ResourceType.ORE, 3);
        wallet.addResource(ResourceType.WHEAT, 2);
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.ORE, 3);
        cost.put(ResourceType.WHEAT, 2);
        assertTrue(wallet.hasEnoughResources(cost));
    }

    @Test
    void hasEnoughResources_byMap_insufficient_returnsFalse() {
        wallet.addResource(ResourceType.ORE, 2);
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.ORE, 3);
        assertFalse(wallet.hasEnoughResources(cost));
    }

    @Test
    void hasEnoughResources_emptyMap_returnsTrue() {
        assertTrue(wallet.hasEnoughResources(new EnumMap<>(ResourceType.class)));
    }

    @Test
    void payCost_sufficientResources_deductsAndReturnsTrue() {
        wallet.addResource(ResourceType.WOOD, 2);
        wallet.addResource(ResourceType.BRICK, 2);
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.WOOD, 1);
        cost.put(ResourceType.BRICK, 1);
        assertTrue(wallet.payCost(cost));
        assertEquals(1, wallet.getResourceAmount(ResourceType.WOOD));
        assertEquals(1, wallet.getResourceAmount(ResourceType.BRICK));
    }

    @Test
    void payCost_insufficientResources_returnsFalseAndKeepsResources() {
        wallet.addResource(ResourceType.WOOD, 1);
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.WOOD, 2);
        assertFalse(wallet.payCost(cost));
        assertEquals(1, wallet.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void payCost_zeroAmountEntry_isSkipped() {
        wallet.addResource(ResourceType.WOOD, 1);
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.WOOD, 0);
        assertTrue(wallet.payCost(cost)); // 0 cost is always payable
        assertEquals(1, wallet.getResourceAmount(ResourceType.WOOD)); // not deducted
    }

    @Test
    void setHiddenTotal_makesWalletHidden() {
        wallet.setHiddenTotal(5);
        assertTrue(wallet.isHidden());
    }

    @Test
    void setHiddenTotal_getTotalCards_returnsHiddenTotal() {
        wallet.setHiddenTotal(7);
        assertEquals(7, wallet.getTotalCards());
    }

    @Test
    void setHiddenTotal_zerosIndividualResources() {
        wallet.addResource(ResourceType.WOOD, 3);
        wallet.setHiddenTotal(5);
        assertEquals(0, wallet.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void setHiddenTotal_negativeValue_clampsToZero() {
        wallet.setHiddenTotal(-3);
        assertEquals(0, wallet.getTotalCards());
    }

    @Test
    void setHiddenTotal_notifiesListener() {
        int[] count = {0};
        wallet.setOnWalletChangedListener(() -> count[0]++);
        wallet.setHiddenTotal(3);
        assertEquals(1, count[0]);
    }

    @Test
    void clearHidden_removesHiddenMode() {
        wallet.setHiddenTotal(5);
        wallet.clearHidden();
        assertFalse(wallet.isHidden());
    }

    @Test
    void clearHidden_getTotalCards_returnsActualAfterClear() {
        wallet.addResource(ResourceType.ORE, 2);
        wallet.setHiddenTotal(10); // zeros actual resources, sets hidden=10
        wallet.clearHidden();
        // Resources were zeroed by setHiddenTotal; actual total is now 0
        assertEquals(0, wallet.getTotalCards());
    }

    @Test
    void removeRandomResource_fromNonEmptyWallet_returnsResource() {
        wallet.addResource(ResourceType.WOOD, 3);
        ResourceType stolen = wallet.removeRandomResource();
        assertNotNull(stolen);
        assertEquals(ResourceType.WOOD, stolen);
        assertEquals(2, wallet.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void removeRandomResource_fromEmptyWallet_returnsNull() {
        assertNull(wallet.removeRandomResource());
    }

    @Test
    void removeRandomResource_multipleTypes_returnsOneOfThem() {
        wallet.addResource(ResourceType.WOOD, 1);
        wallet.addResource(ResourceType.ORE, 1);
        ResourceType stolen = wallet.removeRandomResource();
        assertNotNull(stolen);
        assertTrue(stolen == ResourceType.WOOD || stolen == ResourceType.ORE);
        assertEquals(1, wallet.getTotalCards());
    }

    @Test
    void removeRandomResource_decreasesTotalByOne() {
        wallet.addResource(ResourceType.WHEAT, 5);
        int before = wallet.getTotalCards();
        wallet.removeRandomResource();
        assertEquals(before - 1, wallet.getTotalCards());
    }

    @Test
    void walletChangedListener_isCalledOnAdd() {
        boolean[] called = {false};
        wallet.setOnWalletChangedListener(() -> called[0] = true);
        wallet.addResource(ResourceType.WOOD, 1);
        assertTrue(called[0]);
    }

    @Test
    void walletChangedListener_isCalledOnSuccessfulRemove() {
        wallet.addResource(ResourceType.WOOD, 2);
        boolean[] called = {false};
        wallet.setOnWalletChangedListener(() -> called[0] = true);
        wallet.removeResource(ResourceType.WOOD, 1);
        assertTrue(called[0]);
    }

    @Test
    void noListener_addResource_doesNotThrow() {
        // No listener set — should not throw NPE
        assertDoesNotThrow(() -> wallet.addResource(ResourceType.WOOD, 1));
    }
}
