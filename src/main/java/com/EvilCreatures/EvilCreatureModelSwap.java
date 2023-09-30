package com.EvilCreatures;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.PluginDescriptor;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "Evil Creatures"
)

public class EvilCreatureModelSwap extends Plugin
{

	private static final int[] gnomeIDs = {6094, 6095, 6096, 6081, 6082, 6086, 6087};
	private static final int evilCreatureHeadModelID=16957;
	private static final int evilCreatureBodyModelID=16955;
	private static final int evilCreatureStandingCode=4472;
	private static final int evilCreatureMovingCode=4473;
	private static final int gnomeMovingCode=189;
	private static final int gnomeStandingCode=195;
	private Model evilCreatureHeadModel;
	private Model evilCreatureBodyModel;
	private Animation evilCreatureStandingAnimation;
	private Animation evilCreatureMovingAnimation;
	private ArrayList<ModelPair> currentlyTrackedGnomes;
	private boolean initialized;
	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	@Inject
	private Hooks hooks;
	@Inject
	private ClientThread clientThread;
	@Inject
	private Client client;

	/**
	 * startUp Runs when the plugin starts
	 * @throws Exception
	 */
	@Override
	protected void startUp() throws Exception
	{
		currentlyTrackedGnomes= new ArrayList<ModelPair>();

		hooks.registerRenderableDrawListener(drawListener);

		//Attempts to load the evil Creature Model, if loaded then variable initialized is set to true
		initialized = false;
		clientThread.invoke(() ->
		{
			evilCreatureHeadModel=client.loadModel(evilCreatureHeadModelID);
			evilCreatureBodyModel=client.loadModel(evilCreatureBodyModelID);
			evilCreatureMovingAnimation=	client.loadAnimation(evilCreatureMovingCode);
			evilCreatureStandingAnimation=	client.loadAnimation(evilCreatureStandingCode);
			initialized = ((evilCreatureHeadModel != null) && (evilCreatureBodyModel != null));
		});
	}

	/**
	 * shutDown Runs when the plugin ends
	 * @throws Exception
	 */
	@Override
	protected void shutDown() throws Exception
	{
		//Makes the Gnomes come back
		hooks.unregisterRenderableDrawListener(drawListener);

		clientThread.invoke(() ->
		{
			//Removes all the creatures
			for(ModelPair currentPair : currentlyTrackedGnomes)
			{
				currentPair.getHead().setActive(false);
				currentPair.getBody().setActive(false);
			}
			currentlyTrackedGnomes.clear();
		});
		initialized=false;
	}

	/**
	 * onGameStateChanged Runs when the game state changes
	 * @param gameStateChanged [unused]
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
	}


	/**
	 * onClientTick Runs every client tick.  occurs far more often then game ticks
	 * @param tick
	 */
	@Subscribe
	public void onClientTick(final ClientTick tick)
	{
		//Test is initialized.  If not, code is skipped
		if(!initialized)
		{
			return;
		}

		//Cycles through all gnomes turning them into evil creatures
		for(ModelPair currentPair : currentlyTrackedGnomes)
		{
			RuneLiteObject currentCreatureHead=currentPair.getHead();
			RuneLiteObject currentCreatureBody=currentPair.getBody();
			NPC currentGnome=currentPair.getNPC();
			LocalPoint currentLocation = currentGnome.getLocalLocation();
			int currentWorld= currentGnome.getWorldLocation().getPlane();
			int currentOrientation= currentGnome.getOrientation();

			//Set the runeliteobject to be the evil creature model, with proper orientation/location
			currentCreatureHead.setModel(evilCreatureHeadModel);
			currentCreatureHead.setLocation(currentLocation, currentWorld);
			currentCreatureHead.setOrientation(currentOrientation);
			//makes runelite object visible
			currentCreatureHead.setActive(true);

			currentCreatureBody.setModel(evilCreatureBodyModel);
			currentCreatureBody.setLocation(currentLocation,currentWorld);
			currentCreatureBody.setOrientation(currentOrientation);
			//makes runelite object visible
			currentCreatureBody.setActive(true);

			//Sets the animation for the Body and the head based off if the Gnome is moving
			//Note, we only test the creatures head because the head and body should always be the same
			if (currentGnome.getPoseAnimation()==gnomeStandingCode&&
					(currentCreatureHead.getAnimation()!=evilCreatureStandingAnimation))
			{
				currentCreatureHead.setAnimation(evilCreatureStandingAnimation);
				currentCreatureHead.setShouldLoop(true);
				currentCreatureBody.setAnimation(evilCreatureStandingAnimation);
				currentCreatureBody.setShouldLoop(true);
			}
			else if (currentGnome.getPoseAnimation()==gnomeMovingCode&&
					(currentCreatureHead.getAnimation()!=evilCreatureMovingAnimation))
			{
				currentCreatureHead.setAnimation(evilCreatureMovingAnimation);
				currentCreatureHead.setShouldLoop(true);
				currentCreatureBody.setAnimation(evilCreatureMovingAnimation);
				currentCreatureBody.setShouldLoop(true);
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
		}
	}

	/**
	 * adds an NPC to the list of currently tracked gnomes.
	 * @param inNPC an NPC to be added to the list of currently track gnomes
	 */
	private void addPair(NPC inNPC)
	{
		currentlyTrackedGnomes.add(new ModelPair(inNPC,
				client.createRuneLiteObject(), client.createRuneLiteObject()));
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
			for (ModelPair gnomeTest : currentlyTrackedGnomes)
			{
				if (gnomeTest.getNPC() == goneNPC)
				{
					removeCreature(gnomeTest);
					return;
				}
			}
		}
	}

	/**
	 * Removes the model pair from the list and makes the Creature disappear
	 * @param inModelPair the model pair that is killed off
	 */
	private void removeCreature(ModelPair inModelPair)
	{
		inModelPair.getHead().setActive(false);
		inModelPair.getBody().setActive(false);
		currentlyTrackedGnomes.remove(inModelPair);
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
        for (ModelPair gnomeTest : currentlyTrackedGnomes) {
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
		//if you want everything to be evil creatures make this false
		return false;
	}

	/**
	 * tests if renderable is a Gnome NPC to prevent it from being drawn
	 * @param inRenderable the rednerable object in question
	 * @param isUIElement [unused]
	 * @return returns false if renderable is a NPC gnome, true otherwise
	 */
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
