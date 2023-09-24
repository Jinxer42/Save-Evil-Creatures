package com.EvilCreatures;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.PluginDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "Evil Creatures"
)

public class EvilCreatureModelSwap extends Plugin
{
	@Inject
	private Client client;
	private static final int evilCreatureID=1241;
	private static final int evilCreatureStandingCode=4472;
	private static final int evilCreatureMovingCode=4473;
	private static final int gnomeMovingCode=189;
	private static final int gnomeStandingCode=195;
	private static final int evilCreatureModelID=16959;
	private Model evilCreatureModel;
	private ArrayList<modelPair> currentlyTrackedGnomes;
	@Inject
	private ClientThread clientThread;
	private boolean initialized;
	private static final int[] gnomeIDs = {6094, 6095, 6096, 6081, 6082, 6086, 6087};
	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	@Inject
	private Hooks hooks;
	@Inject
	private ExampleConfig config;
	private Animation evilCreatureStandingAnimation;
	private Animation evilCreatureMovingAnimation;
	private long counter;
	private int currentHeight;

	/**
	 * startUp Runs when the plugin starts
	 * @throws Exception
	 */
	@Override
	protected void startUp() throws Exception
	{
		counter=0;
		log.info("Example started!");
		currentlyTrackedGnomes= new ArrayList<modelPair>();

		hooks.registerRenderableDrawListener(drawListener);

		final Instant loadTimeOutInstant = Instant.now().plus(Duration.ofSeconds(60));

		//Attempts to load the evil Creature Model, if loaded then variable initalized is set to true
		initialized = false;
		clientThread.invoke(() ->
		{
			evilCreatureModel=client.loadModel(evilCreatureModelID);
			evilCreatureMovingAnimation=	client.loadAnimation(evilCreatureMovingCode);
			evilCreatureStandingAnimation=	client.loadAnimation(evilCreatureStandingCode);
			initialized = (evilCreatureModel != null);
		});
	}

	/**
	 * shutDown Runs when the plugin ends
	 * @throws Exception
	 */
	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
		hooks.unregisterRenderableDrawListener(drawListener);
	}

	/**
	 * onGameStateChanged Runs when the game state changes
	 * @param gameStateChanged
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{

		//Redoes the list due to issues with chunk loading, or something.  Covers a host of possible issues
		if(initialized)
		{
			currentlyTrackedGnomes.clear();
			List<NPC> allNPCs = client.getNpcs();
			for (NPC possibleGnome : allNPCs) {
				if (isGnome(possibleGnome)) {
					addPair(possibleGnome);
				}
			}
		}

		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}

	/**
	 * onClientTick Runs every client tick.  occurs far more ofthen then game ticks
	 * @param tick
	 */
	@Subscribe
	public void onClientTick(final ClientTick tick)
	{
		//Test is initalized.  If not, code is skipped
		if(!initialized)
		{
			log.info("You never itlaized Error: 228955682");
			return;
		}
		//Cycles through all gnomes turning them into evil creatures
		for(modelPair currentPair : currentlyTrackedGnomes)
		{
			RuneLiteObject currentCreature=currentPair.getEvilCreature();
			NPC currentGnome=currentPair.getNPC();

			//Set the runeliteobject to be the evil creature model, with proper orientation/location
			currentCreature.setModel(evilCreatureModel);
			currentCreature.setLocation(currentGnome.getLocalLocation(), currentGnome.getWorldLocation().getPlane());
			currentCreature.setOrientation(currentGnome.getOrientation());
			//makes runelite object visible
			currentCreature.setActive(true);

			//TODO animations are not smooth, they look disjointed
			//Attempts to make the animation for the evil creatures.  But fails
			if (currentGnome.getPoseAnimation()==gnomeStandingCode&&
					(currentCreature.getAnimation()!=evilCreatureStandingAnimation))
			{
				currentCreature.setAnimation(evilCreatureStandingAnimation);
				currentCreature.setShouldLoop(true);
			}
			else if (currentGnome.getPoseAnimation()==gnomeMovingCode&&
					(currentCreature.getAnimation()!=evilCreatureMovingAnimation))
			{
				currentCreature.setAnimation(evilCreatureMovingAnimation);
				currentCreature.setShouldLoop(true);
			}
		}
	}

	/**
	 * Runs everytime a NPC is spawned
	 * @param inNPCSpawned the spawn NPC that was registered
	 */
	@Subscribe
	public void onNpcSpawned(NpcSpawned inNPCSpawned)
	{
		//Checks if the spawned NPC is a new gnome, if it is, is added to the list)
		NPC newNPC = inNPCSpawned.getNpc();
		if(isNewGnome(newNPC))
		{
			addPair(newNPC);
			//currentlyTrackedGnomes.add(new modelPair(newNPC, client.createRuneLiteObject()));
		}
		//For test purposes Only, should be deleted on live build
		else if(!isGnome(newNPC))
		{
			log.info("ID " + newNPC.getId());
		}
	}

	/**
	 * adds an NPC to the list of currently tracked gnomes.
	 * @param inNPC an NPC to be added to the list of currently track gnomes
	 */
	private void addPair(NPC inNPC)
	{
		currentlyTrackedGnomes.add(new modelPair(inNPC, client.createRuneLiteObject()));
	}

	/**
	 * checks if despawned NPC was on list, if so, removes from the list.
	 * @param inNPCDespawned the NPC that just deswpaned
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned inNPCDespawned)
	{
		NPC goneNPC = inNPCDespawned.getNpc();
		if(isOnList(goneNPC))
		{
			for (modelPair gnomeTest : currentlyTrackedGnomes)
			{
				if (gnomeTest.getNPC() == goneNPC) {

					gnomeTest.getEvilCreature().setActive(false);
					currentlyTrackedGnomes.remove(gnomeTest);
					return;
				}
			}
			log.info("Should never be excuted. Error: 482294");
		}
	}

	/**
	 * tests whether an NPC is a new gnome
	 * @param inNPC NPC that is being tested for new gnomehood
	 * @return true is NPC is a new gnome, false otherwise
	 */
	private boolean isNewGnome(NPC inNPC)
	{
		return this.isGnome(inNPC) && !this.isOnList(inNPC);
	}

	/**
	 * tests whether an NPC is on the list of currently tracked gnomes
	 * @param inNPC NPC that is being tested of listhood
	 * @return return true if it is on the list, false otherwise
	 */
	private boolean isOnList(NPC inNPC)
	{
       //TODO never used this kind of loop before.. idk if i like it.
        for (modelPair gnomeTest : currentlyTrackedGnomes) {
            if (gnomeTest.getNPC() == inNPC) {
                return true;
            }
        }
		return false;
	}

	/**
	 * tests whether an NPC is a gnome
	 * @param inNPC NPC that is being tested for gnomehood
	 * @return true is NPC is a gnome, false otherwise
	 */
	private boolean isGnome(NPC inNPC)
	{
        for (int gnomeID : gnomeIDs) {
            if (gnomeID == inNPC.getId())
			{
                return true;
            }
        }
		return false;
	}

	/**
	 * Prints lsit of currently tracked gnomes.  For testing purposes only
	 */
	private void printList()
	{
		for (modelPair gnomeTest : currentlyTrackedGnomes)
		{
			log.info(gnomeTest.getNPC().getName());
		}
	}

	//Thing I really don't understand well enough to properly document...
	public boolean shouldDraw(Renderable inRenderable, boolean isUIElement)
	{
		if(inRenderable instanceof NPC)
		{
			NPC currentRenderable = (NPC) inRenderable;
			return !isGnome(currentRenderable);
		}
		return true;
	}
}




