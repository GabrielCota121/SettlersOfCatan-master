package com.example.model.state;

import com.example.model.board.BoardFactory;
import com.example.model.board.Edge;
import com.example.model.board.Vertex;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import com.example.model.trade.TradeOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTradeStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player proposer;
    private Player player2;
    private Player player3;
    private CatanGameManager gameManager;
    private TradeOffer offer;
    private PlayerTradeState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        proposer = new Player(1, "Marcelle", "red");
        player2 = new Player(2, "Lucas", "blue");
        player3 = new Player(3, "Gabriel", "green");
        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(proposer, player2, player3), SILENT_LOGGER);

        Map<ResourceType, Integer> offered = new EnumMap<>(ResourceType.class);
        offered.put(ResourceType.WOOD, 1);
        Map<ResourceType, Integer> requested = new EnumMap<>(ResourceType.class);
        requested.put(ResourceType.ORE, 1);

        offer = new TradeOffer(proposer, offered, requested);
        state = new PlayerTradeState(offer, List.of(proposer, player2, player3));
        turn = new Turn(proposer, gameManager);
        turn.setState(state);

        proposer.getWallet().addResource(ResourceType.WOOD, 1);
    }

    @Test
    void getName_whenWaitingForResponse_includesTargetPlayerName() {
        assertTrue(state.getName().contains(state.getCurrentTargetPlayer().getName()));
    }

    @Test
    void getName_whenWaitingForProposer_returnsClosingDeal() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);
        state.registerResponse(true, turn);
        assertTrue(state.isWaitingForProposer());
        assertTrue(state.getName().contains("Fechando Acordo"));
    }

    @Test
    void getCurrentTargetPlayer_initiallyFirstNonProposer() {
        Player target = state.getCurrentTargetPlayer();
        assertNotEquals(proposer, target);
    }

    @Test
    void isWaitingForProposer_initiallyFalse() {
        assertFalse(state.isWaitingForProposer());
    }

    @Test
    void registerResponse_accepted_addedToAcceptanceList() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);

        assertTrue(offer.getAcceptedBy().contains(player2));
    }

    @Test
    void registerResponse_rejected_notAddedToAcceptanceList() {
        state.registerResponse(false, turn);

        assertFalse(offer.getAcceptedBy().contains(player2));
    }

    @Test
    void registerResponse_accepted_butCannotAfford_notAdded() {
        state.registerResponse(true, turn);

        assertFalse(offer.getAcceptedBy().contains(player2));
    }

    @Test
    void registerResponse_allRejected_stateBecomesMainState() {
        state.registerResponse(false, turn);
        state.registerResponse(false, turn);

        assertInstanceOf(MainState.class, turn.getState());
    }

    @Test
    void registerResponse_allResponded_withAcceptance_waitingForProposer() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);
        state.registerResponse(false, turn);

        assertTrue(state.isWaitingForProposer());
    }

    @Test
    void executeTrade_transfersResourcesToProposer() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);
        state.registerResponse(false, turn);

        state.executeTrade(player2, turn);

        assertEquals(1, proposer.getWallet().getResourceAmount(ResourceType.ORE));
    }

    @Test
    void executeTrade_transfersResourcesToPartner() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);
        state.registerResponse(false, turn);

        state.executeTrade(player2, turn);

        assertEquals(1, player2.getWallet().getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void executeTrade_proposerLosesOfferedResources() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);
        state.registerResponse(false, turn);

        state.executeTrade(player2, turn);

        assertEquals(0, proposer.getWallet().getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void executeTrade_stateBecomesMainState() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);
        state.registerResponse(false, turn);

        state.executeTrade(player2, turn);

        assertInstanceOf(MainState.class, turn.getState());
    }

    @Test
    void executeTrade_withNonAcceptingPartner_doesNothing() {
        player2.getWallet().addResource(ResourceType.ORE, 1);
        state.registerResponse(true, turn);
        state.registerResponse(false, turn);

        int woodBefore = proposer.getWallet().getResourceAmount(ResourceType.WOOD);
        state.executeTrade(player3, turn);

        assertEquals(woodBefore, proposer.getWallet().getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void cancelTrade_stateBecomesMainState() {
        state.cancelTrade(turn);

        assertInstanceOf(MainState.class, turn.getState());
    }

    @Test
    void getOffer_returnsCorrectOffer() {
        assertSame(offer, state.getOffer());
    }

    @Test
    void buildSettlement_returnsFalse() {
        assertFalse(state.buildSettlement(new Vertex(0, 0), turn));
    }

    @Test
    void buildRoad_returnsFalse() {
        assertFalse(state.buildRoad(new Edge(new Vertex(0, 0), new Vertex(1, 0)), turn));
    }

    @Test
    void buildCity_returnsFalse() {
        assertFalse(state.buildCity(new Vertex(0, 0), turn));
    }

    @Test
    void buyDevelopmentCard_returnsFalse() {
        assertFalse(state.buyDevelopmentCard(turn));
    }

    @Test
    void rollDice_returnsFalse() {
        assertFalse(state.rollDice(turn));
    }

    @Test
    void playDevelopmentCard_returnsFalse() {
        assertFalse(state.playDevelopmentCard(null, turn));
    }

    @Test
    void canEndTurn_returnsFalse() {
        assertFalse(state.canEndTurn());
    }

    @Test
    void canRollDice_returnsFalse() {
        assertFalse(state.canRollDice());
    }

    @Test
    void endTurn_returnsFalse() {
        assertFalse(state.endTurn(turn));
    }
}
