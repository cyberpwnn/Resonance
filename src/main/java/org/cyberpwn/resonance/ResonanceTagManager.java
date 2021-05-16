package org.cyberpwn.resonance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.BossInfoClient;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.cyberpwn.resonance.config.QueueConfig;
import org.cyberpwn.resonance.config.ResonanceConfig;
import org.cyberpwn.resonance.config.TagCondition;
import org.cyberpwn.resonance.config.TagConditionMode;
import org.cyberpwn.resonance.player.FilePlayer;
import org.cyberpwn.resonance.player.Player;

import java.io.*;
import java.util.*;

public class ResonanceTagManager {
    public static final String[] OBF_MAP_BOSS_INFOS = { "g", "field_184060_g", "mapBossInfos" };
    private List<String> tags;
    private File configFile;
    private long lastMod = -1;
    private QueueConfig config;
    private Map<String, String> possibleTags;
    private Map<String, Long> playtimes;
    private List<Player> playable;
    private int cid = 0;
    private int ocid = 0;
    private boolean configChanged;
    private double conflict = 0;
    private double lushness = 0;
    private Random random;

    public ResonanceTagManager()
    {
        configChanged = false;
        random = new Random();
        tags = new ArrayList<>();
        playtimes = new HashMap<>();
        playable = new ArrayList<>();
        configFile = new File(Resonance.folder, "conditions.json");
        updatePlayable();
    }

    public double getConflict()
    {
        return conflict;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setPlaytime(Player p)
    {
        playtimes.put(p.getId(), p.getTimecode());
    }

    public List<Player> getPlayable()
    {
        return playable;
    }

    public QueueConfig getConfig()
    {
        if(configFile.lastModified() != lastMod || config == null)
        {
            lastMod = configFile.lastModified();
            config = createConfig();
            configChanged = true;
            System.out.println("Configuration Reloaded");
            cid++;
        }
        
        return config;
    }

    private QueueConfig createConfig() {
        QueueConfig config = new QueueConfig();

        if(configFile.exists())
        {
            try
            {
                StringBuilder j = new StringBuilder();
                BufferedReader bu = new BufferedReader(new FileReader(configFile));
                String l;

                while((l = bu.readLine()) != null)
                {
                    j.append(l);
                }

                bu.close();
                return new Gson().fromJson(j.toString(), QueueConfig.class);
            }

            catch(Throwable e)
            {
                e.printStackTrace();
            }
        }

        if(!configFile.exists())
        {
            for(String i : getPossibleTags().keySet())
            {
                TagCondition tc = new TagCondition();
                tc.setMode(TagConditionMode.OR);
                tc.getWhen().add(i);
                config.getConditions().add(tc);
                try {
                    PrintWriter pw = new PrintWriter(configFile);
                    pw.println(new GsonBuilder().setPrettyPrinting().create().toJson(config));
                    pw.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return config;
    }

    public void untag(String s)
    {
        tags.remove(s);
    }

    public double getLushness()
    {
        return lushness;
    }

    public void tag(String s)
    {
        if(!s.contains(s))
        {
            tags.add(s);
        }
    }

    public void increaseConflict(double pts)
    {
        conflict+= pts;
    }

    public void writeTagGuide()
    {
        Resonance.folder.mkdirs();
        File f = new File(Resonance.folder, "help.txt");
        try {
            PrintWriter pw = new PrintWriter(f);
            pw.println("Depending on what the player is doing ingame, different triggers will activate and deactivate.");
            pw.println("Multiple triggers can be active at once. When triggers change, the queue of music changes.");
            pw.println("Below is a list of triggers that happen and their description.");
            pw.println();
            pw.println("=== TAG CONDITIONS ===");
            pw.println("The json config contains all tags by default as separate conditions. If you assume the tag name is 'biome_plains'");
            pw.println("biome_plains  = Condition met if the tag is active");
            pw.println("!biome_plains = Condition is met if tag is INACTIVE");
            pw.println("biome_*       = If any tag active starts with biome_ (such as bione_plains) or (biome_desert)");
            pw.println("!biome_mesa*  = If any tag inactive starts with biome_mesa (such as bione_mesa) or (biome_mesa_rock)");
            pw.println();
            pw.println("=== TAGS ===");

            for(String i : getPossibleTags().keySet())
            {
                pw.println(i + " - " + getPossibleTags().get(i));
            }

            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> getPossibleTags() {
        if(possibleTags != null)
        {
            return possibleTags;
        }

        Map<String, String> t = new HashMap<>();

        t.put("startup", "Forge is starting up");
        t.put("startup.preinit", "Forge is starting up (pre)");
        t.put("startup.init", "Forge is starting up (mid)");
        t.put("startup.postinit", "Forge is starting up (post)");
        t.put("menu", "Game is in the main menu");
        t.put("world", "Anytime you are ingame (in any world)");
        t.put("humid_wet", "Wet biome");
        t.put("humid_dry", "Dry biome");
        t.put("humid_mild", "Normal Humidity ");
        t.put("temp_freezing", "Frozen biome");
        t.put("temp_chilly", "Cold biome");
        t.put("temp_warm", "Warm biome");
        t.put("temp_hot", "Hot biome");
        t.put("temp_superhot", "Very hot biome");
        t.put("height_sohigh", "At or above 200");
        t.put("height_high", "Below 200");
        t.put("height_sealevel", "Below 94");
        t.put("height_low", "Below 60");
        t.put("height_bedrock", "Below 35");
        t.put("hp_low", "HP Below 30%");
        t.put("fp_low", "Food below 30%");
        t.put("dead", "Death screen");
        t.put("time_night", "Night time...");
        t.put("storm", "If it's raining in the world & the current biome supports rain");
        t.put("thunder", "If it's thundering in a biome where rain / snow can happen");
        t.put("time_day", "Day time...");
        t.put("light_dark", "Darkness");
        t.put("light_mid", "Middle range light");
        t.put("light_bright", "High light...");
        t.put("lush_dense", "Lots of foliage, & greens");
        t.put("lush_normal", "Medium amounts of foliage, & greens");
        t.put("lush_barren", "Little to no foliage / greens");

        for(Biome i : ForgeRegistries.BIOMES.getValuesCollection())
        {
            t.put("biome_"+i.getRegistryName().getResourcePath(), "When in the " + i.getBiomeName() + " Biome");
        }

        for(DimensionType i : DimensionManager.getRegisteredDimensions().keySet())
        {
            t.put("dim" + i.getId(), "When in " + i.getName() + " Dimension.");
        }

        possibleTags = t;
        return getPossibleTags();
    }

    public boolean isValid(TagCondition c)
    {
        if(c.getWhen().isEmpty())
        {
            return false;
        }

        for(String i : c.getWhen())
        {
            if(!isValid(i))
            {
                if(c.getMode().equals(TagConditionMode.AND))
                {
                    return false;
                }
            }

            else if(c.getMode().equals(TagConditionMode.OR))
            {
                return true;
            }
        }

        return c.getMode().equals(TagConditionMode.AND);
    }

    private boolean isValid(String i) {
        if(i.contains("|"))
        {
            for(String j : i.split("\\Q|\\E"))
            {
                if(isValid(j))
                {
                    return true;
                }
            }
        }

        else if(i.contains("&"))
        {
            for(String j : i.split("\\Q&\\E"))
            {
                if(!isValid(j))
                {
                    return false;
                }
            }

            return true;
        }

        String s = i;
        boolean not = false;

        if(s.startsWith("!"))
        {
            not = true;
            s = s.substring(1);
        }

        return not != tags.contains(s);
    }

    public void updateTags()
    {
        List<java.lang.String> tc = new ArrayList<>();
        tc.addAll(tags);
        tags.clear();

        if(Minecraft.getMinecraft().world == null)
        {
            tags.add(Resonance.startupTag);

            if(Resonance.startupTag.contains("startup."))
            {
                tags.add("startup");
            }
        }

        else
        {
            try
            {
                tags.add("world");
                EntityPlayerSP p = Minecraft.getMinecraft().player;
                updateBiomeTag(p);
                updateHeightTag(p);
                updateVitalsTag(p);
                updateTimeTag();
                updateLightTag(p);
                updateDimensionTag();
                updateConflictTag();
                updateBossTag();
                updateLush();
                updateWeather();
                updateSide();
            }

            catch(Throwable e)
            {
                e.printStackTrace();
            }
        }

        if(configChanged || !tags.equals(tc) || cid != ocid)
        {
            ocid = cid;
            updatePlayable();
        }
    }

    private void updateSide() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        BlockPos head = p.getPosition().add(0, 1, 0);

        if(underCeiling(head)
                &&underCeiling(head.add(1, 0, 0))
                &&underCeiling(head.add(0, 0, 1))
                &&underCeiling(head.add(-1, 0, 0))
                &&underCeiling(head.add(0, 0, -1))
        )
        {
            tags.add("inside");
        }

        else
        {
            tags.add("outside");
        }
    }

    private boolean underCeiling(BlockPos p)
    {
        for(int i = 0; i < 16; i++)
        {
            if(i+p.getY() > 255)
            {
                break;
            }

            if(isSolid(Minecraft.getMinecraft().world.getBlockState(p.add(0, i, 0))))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isSolid(IBlockState b)
    {
        if((b.isFullBlock() || b.isFullCube()) && !b.isTranslucent())
        {
            return true;
        }

        return false;
    }

    private void updateWeather() {
        try
        {
            EntityPlayerSP p = Minecraft.getMinecraft().player;
            Biome in = Minecraft.getMinecraft().world.getBiome(new BlockPos(p.posX, p.posY, p.posZ));

            if(Minecraft.getMinecraft().world.isRaining())
            {
                if(in.canRain())
                {
                    tags.add("storm");
                }
            }

            if(Minecraft.getMinecraft().world.isThundering())
            {
                if(in.canRain() || in.getRainfall() > 0)
                {
                    tags.add("thunder");
                }
            }
        }

        catch(Throwable e)
        {

        }
    }

    private void updateLush() {
        try
        {
            int lushScore = 0;
            EntityPlayerSP s = Minecraft.getMinecraft().player;
            BlockPos b = s.getPosition();

            for(int i = 0; i < ResonanceConfig.blockChecks; i++)
            {
                BlockPos bp = b.add(random.nextInt(ResonanceConfig.blockCheckRadius * 2) - ResonanceConfig.blockCheckRadius,
                        random.nextInt(ResonanceConfig.blockCheckRadius) - ResonanceConfig.blockCheckRadius/2,
                        random.nextInt(ResonanceConfig.blockCheckRadius * 2) - ResonanceConfig.blockCheckRadius);
                Block block = s.world.getBlockState(bp).getBlock();
                int flam = net.minecraft.init.Blocks.FIRE.getFlammability(block);
                lushScore += flam/25;

                if(block instanceof BlockFlower) {
                    lushScore += 35;
                }

                if(block instanceof BlockLeaves) {
                    lushScore += 125;
                }

                if(block instanceof BlockLog) {
                    lushScore += 25;
                }

                if(block instanceof BlockVine) {
                    lushScore += 145;
                }

                if(block instanceof BlockGrass)
                {
                    lushScore += 7;
                }

                if(block instanceof BlockTallGrass)
                {
                    lushScore += 20;
                }
            }

            lushScore /= ResonanceConfig.blockChecks;
            lushness += lushScore / 80.0;
            lushness *= 0.85;
            lushness = lushness > 1 ? 1 : lushness < 0 ? 0 : lushness;
        }

        catch(Throwable e)
        {

        }

        if(lushness > 0.65)
        {
            tags.add("lush_dense");
        }

        if(lushness >= 0.25)
        {
            tags.add("lush_normal");
        }

        if(lushness < 0.25)
        {
            tags.add("lush_barren");
        }
    }

    private void updateBossTag() {
        try
        {
            GuiBossOverlay bossOverlay = Minecraft.getMinecraft().ingameGUI.getBossOverlay();
            Map<UUID, BossInfoClient> map = ReflectionHelper.getPrivateValue(GuiBossOverlay.class, bossOverlay, OBF_MAP_BOSS_INFOS);
            if(!map.isEmpty()) {
                BossInfoClient first = map.get(map.keySet().iterator().next());
                ITextComponent comp = first.getName();
                String type = "";

                if(comp instanceof TextComponentString) {
                    type = comp.getStyle().getHoverEvent().getValue().getUnformattedComponentText();
                    type = type.substring(type.indexOf("type:\"") + 6, type.length() - 2);
                } else if(comp instanceof TextComponentTranslation) {
                    type = ((TextComponentTranslation) comp).getKey();
                    if(type.startsWith("entity.") && type.endsWith(".name"))
                        type = type.substring(7, type.length() - 5);
                }

                if(type.equals("minecraft:wither") || type.equals("EnderDragon")) {
                    tags.add("boss");
                }
            }
        }

        catch(Throwable e)
        {

        }
    }

    private void updateConflictTag() {
        if(conflict > 100)
        {
            conflict = 100;
        }

        if(conflict < 0)
        {
            conflict = 0;
        }

        conflict *= 0.865;

        if(conflict > 15)
        {
            tags.add("combat");
        }
    }

    public void updatePlayable() {
        synchronized (playable)
        {
            playable.clear();

            for(TagCondition i : getConfig().getConditions())
            {
                if(isValid(i))
                {
                    queue(i);
                }
            }
        }
    }

    private void queue(TagCondition tc) {
        loading: for(String i : tc.getPlay())
        {
            try {
                Player p = new FilePlayer(new File(Resonance.music, i), getTimecode(new File(Resonance.music, i).getAbsolutePath()));
                p.setPriority(tc.getPriority());
                p.setSudden(tc.isSudden());

                for(Player j : playable)
                {
                    if(j.getId().equals(p.getId()))
                    {
                        continue loading;
                    }
                }

                playable.add(p);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private long getTimecode(String i) {
        return playtimes.compute(i, (k,v) -> v == null ? 0 : v);
    }

    private void updateDimensionTag() {
        tags.add("dim" + Minecraft.getMinecraft().world.getWorldType().getId());
    }

    private void updateLightTag(EntityPlayerSP p) {
        float b = p.world.getCombinedLight(new BlockPos(p.posX, p.posY, p.posZ), p.world.getSkylightSubtracted()) / 15f;

        if(b > 0.7)
        {
            tags.add("light_bright");
        }

        else if(b > 0.4)
        {
            tags.add("light_mid");
        }

        else
        {
            tags.add("light_dark");
        }
    }

    private void updateTimeTag() {
        if(Minecraft.getMinecraft().world.getWorldTime() > 0 && Minecraft.getMinecraft().world.getWorldTime() < 12000)
        {
            tags.add("time_day");
        }

        else
        {
            tags.add("time_night");
        }
    }

    private void updateVitalsTag(EntityPlayerSP p) {
        if(p.getHealth() / p.getMaxHealth() <= 0.3)
        {
            tags.add("hp_low");
        }

        if(p.getFoodStats().getFoodLevel() / 20.0 <= 0.3)
        {
            tags.add("fp_low");
        }

        if(p.isDead)
        {
            tags.add("dead");
        }
    }

    private void updateHeightTag(EntityPlayerSP p) {
        int pos = (int) p.posY;
        if(pos > 200)
        {
            tags.add("height_sohigh");
        }

        else if(pos > 94)
        {
            tags.add("height_high");
        }

        else if(pos > 60)
        {
            tags.add("height_sealevel");
        }

        else if(pos > 35)
        {
            tags.add("height_low");
        }

        else {
            tags.add("height_bedrock");
        }
    }

    private void updateBiomeTag(EntityPlayerSP p) {
        Biome in = Minecraft.getMinecraft().world.getBiome(new BlockPos(p.posX, p.posY, p.posZ));
        tags.add("biome_" + in.getRegistryName().getResourcePath());
        updateHumidityTag(in);
        updateTemperatureTag(in);
    }

    private void updateTemperatureTag(Biome in) {
        if(in.getDefaultTemperature() < 0)
        {
            tags.add("temp_freezing");
        }
        else if(in.getDefaultTemperature() < 0.4)
        {
            tags.add("temp_chilly");
        }
        else if(in.getDefaultTemperature() > 1)
        {
            tags.add("temp_superhot");
        }
        else if(in.getDefaultTemperature() > 0.8)
        {
            tags.add("temp_hot");
        }
        else if(in.getDefaultTemperature() > 0.4)
        {
            tags.add("temp_warm");
        }
    }

    private void updateHumidityTag(Biome in) {
        if(in.getRainfall() > 0.75)
        {
            tags.add("humid_wet");
        }
        else if(in.getRainfall() > 0.35 && in.getRainfall() < 0.75)
        {
            tags.add("humid_mild");
        }
        else if(in.getRainfall() < 0.35)
        {
            tags.add("humid_dry");
        }
    }
}
