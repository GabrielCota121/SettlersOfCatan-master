package com.catan.model.state;

import com.catan.model.board.BoardFactory;
import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.game.Bank;
import com.catan.model.game.CatanGameManager;
import com.catan.model.game.ResourceType;
import com.catan.model.game.Turn;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YearOfPlentyStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player player;
    private CatanGameManager gameManager;
    private Bank bank;
    private Turn turn;
    private ITurnState previousState;
    private YearOfPlentyState state;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Test", "red");
        gameManager = new CatanGameManager(BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
        bank = gameManager.getBank();
        previousState = new MainState();
        state = new YearOfPlentyState(player, previousState);
        turn = new Turn(player, gameManager);
        turn.setState(state);
    }

    @Test
    void getNameReturnsYearOfPlentyState() {
        assertEquals("YearOfPlentyState", state.getName());
    }

    @Test
    void chooseResources_differentTypes_playerReceivesBoth() {
        state.chooseResources(ResourceType.WOOD, ResourceType.WOOL, turn);

        assertEquals(1, player.getWallet().getResourceAmount(ResourceType.WOOD));
        assertEquals(1, player.getWallet().getResourceAmount(ResourceType.WOOL));
    }

    @Test
    void chooseResources_differentTypes_bankLosesBoth() {
        int bankWoodBefore = bank.getWallet().getResourceAmount(ResourceType.WOOD);
        int bankWoolBefore = bank.getWallet().getResourceAmount(ResourceType.WOOL);

        state.chooseResources(ResourceType.WOOD, ResourceType.WOOL, turn);

        assertEquals(bankWoodBefore - 1, bank.getWallet().getResourceAmount(ResourceType.WOOD));
        assertEquals(bankWoolBefore - 1, bank.getWallet().getResourceAmount(ResourceType.WOOL));
    }

    @Test
    void chooseResources_sameType_bankHasSufficient_playerReceivesTwo() {
        state.chooseResources(ResourceType.ORE, ResourceType.ORE, turn);

        assertEquals(2, player.getWallet().getResourceAmount(ResourceType.ORE));
    }

    @Test
    void chooseResources_sameType_bankHasOne_playerReceivesOne() {
        bank.getWallet().removeResource(ResourceType.BRICK, 18); // 19 - 18 = 1

        state.chooseResources(ResourceType.BRICK, ResourceType.BRICK, turn);

        assertEquals(1, player.getWallet().getResourceAmount(ResourceType.BRICK));
        assertEquals(0, bank.getWallet().getResourceAmount(ResourceType.BRICK));
    }

    @Test
    void chooseResources_sameType_bankHasNone_playerReceivesNothing() {
        bank.getWallet().removeResource(ResourceType.WHEAT, 19);

        state.chooseResources(ResourceType.WHEAT, ResourceType.WHEAT, turn);

        assertEquals(0, player.getWallet().getResourceAmount(ResourceType.WHEAT));
    }

    @Test
    void chooseResources_differentTypes_bankLacksSecond_playerOnlyReceivesAvailable() {
        bank.getWallet().removeResource(ResourceType.WOOL, 19);

        state.chooseResources(ResourceType.WOOD, ResourceType.WOOL, turn);

        assertEquals(1, player.getWallet().getResourceAmount(ResourceType.WOOD));
        assertEquals(0, player.getWallet().getResourceAmount(ResourceType.WOOL));
    }

    @Test
    void chooseResources_restoresPreviousState() {
        state.chooseResources(ResourceType.WOOD, ResourceType.WHEAT, turn);

        assertInstanceOf(MainState.class, turn.getState());
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
