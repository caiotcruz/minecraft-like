package com.mcraft.audio;

public enum SoundEvent {
    
    //Break
    BLOCK_BREAK_GRASS ("sounds/block/grass/break1.ogg"),
    BLOCK_BREAK_DIRT ("sounds/block/dirt/break1.ogg"),
    BLOCK_BREAK_LEAVES ("sounds/block/leaves/break1.ogg"),
    BLOCK_BREAK_SAND("sounds/block/sand/break1.ogg"),
    BLOCK_BREAK_STONE ("sounds/block/stone/break1.ogg"),
    BLOCK_BREAK_WOOD  ("sounds/block/wood/break1.ogg"),

    //Hit
    BLOCK_HIT_GRASS   ("sounds/block/grass/hit1.ogg"),
    BLOCK_HIT_STONE   ("sounds/block/stone/hit1.ogg"),
    BLOCK_HIT_WOOD    ("sounds/block/wood/hit1.ogg"),
    
    //Place
    BLOCK_PLACE_WOOD       ("sounds/block/wood/place1.ogg"),
    BLOCK_PLACE_GRASS      ("sounds/block/wood/place1.ogg"),
    BLOCK_PLACE_DIRT       ("sounds/block/wood/place1.ogg"),
    BLOCK_PLACE_STONE      ("sounds/block/wood/place1.ogg"),
    
    //Steps
    STEP_GRASS        ("sounds/step/grass1.ogg"),
    STEP_SAND         ("sounds/step/grass1.ogg"),
    STEP_WOOD         ("sounds/step/grass1.ogg"),
    STEP_STONE        ("sounds/step/stone1.ogg"),
    
    //Ambient / Music
    AMBIENT_CAVE      ("sounds/ambient/cave/cave1.ogg"),
    MUSIC_DAY         ("sounds/ambient/music/music_day.ogg"),
    MUSIC_NIGHT       ("sounds/ambient/music/music_night.ogg");

    public final String path;
    SoundEvent(String p) { this.path = p; }
}