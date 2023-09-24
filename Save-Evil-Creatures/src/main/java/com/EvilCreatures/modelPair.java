package com.EvilCreatures;

import net.runelite.api.NPC;
import net.runelite.api.RuneLiteObject;

public class modelPair {

    private NPC gnome;
    private RuneLiteObject evilCreature;
    protected modelPair (NPC inNPC, RuneLiteObject inRuneLiteObject)
    {
        setNPC(inNPC);
        setRuneLiteObject(inRuneLiteObject);
    }
    public boolean sameNPC(modelPair inModelPair)
    {
        return this.getNPC()==inModelPair.getNPC();
    }

    private void setNPC(NPC inNPC)
    {
        gnome = inNPC;
    }
    private void setRuneLiteObject(RuneLiteObject inRuneLiteObject)
    {
        evilCreature=inRuneLiteObject;
    }

    protected NPC getNPC()
    {
        return gnome;
    }
    protected RuneLiteObject getEvilCreature()
    {
        return evilCreature;
    }
}
