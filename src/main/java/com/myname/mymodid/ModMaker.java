package com.myname.mymodid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;

/**
 * Criador de Mod v1.0 - editor 2D de blocos que gera o codigo-fonte de um mod para Minecraft 1.7.10.
 * Comando: /modmaker (ou /mm). Os projetos ficam em .minecraft/modmaker/projetos e o codigo gerado em .minecraft/modmaker.
 */
@Mod(modid = ModMaker.MODID, name = "Criador de Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class ModMaker {

    public static final String MODID = "modmaker";

    // ============================================================================================
    // EVENTOS (o que inicia um script)
    // ============================================================================================
    static final int EV_JOIN = 0, EV_TIME = 1, EV_BREAK = 2, EV_KILL = 3, EV_HURT = 4, EV_CMD = 5, EV_ITEM = 6, EV_BLOCK = 7,
        EV_JUMP = 8, EV_DIE = 9, EV_RESPAWN = 10;
    static final String[] EV_NAME = {"Ao entrar no mundo", "A cada N segundos", "Ao quebrar um bloco", "Ao matar um mob",
        "Ao receber dano", "Ao usar o comando /nome", "Ao clicar com item do mod", "Ao clicar no bloco do mod", "Ao pular",
        "Ao morrer", "Ao renascer"};

    // ============================================================================================
    // BLOCOS DE FUNCAO (44)
    // ============================================================================================
    static final int B_MSG = 0, B_BROADCAST = 1, B_GIVE = 2, B_HEAL = 3, B_DAMAGE = 4, B_FOOD = 5, B_XP = 6, B_POTION = 7,
        B_TP = 8, B_TPSPAWN = 9, B_SETBLOCK = 10, B_AIR = 11, B_EXPLODE = 12, B_LIGHTNING = 13, B_SPAWN = 14, B_SOUND = 15,
        B_TIME = 16, B_WEATHER = 17, B_FIRE = 18, B_LAUNCH = 19, B_FLY = 20, B_SPEED = 21, B_CLEARINV = 22, B_KILL = 23,
        B_CANCEL = 24, B_CALL = 25, B_WAIT = 26,
        B_IF_HEALTH = 27, B_IF_SNEAK = 28, B_IF_TIME = 29, B_IF_RAIN = 30, B_IF_ITEM = 31, B_IF_VAR = 32, B_IF_HEIGHT = 33,
        B_IF_CHANCE = 34, B_ELSE = 35, B_END = 36, B_REPEAT = 37,
        B_VSET = 38, B_VADD = 39, B_VSUB = 40, B_VMUL = 41, B_VRAND = 42, B_VSHOW = 43;
    static final int NB = 44;

    static final String[] BLABEL = new String[NB];
    static final int[] BCAT = new int[NB];
    static final String[][] BSPEC = new String[NB][];
    static final String[][] BDEF = new String[NB][];
    static final String[] CATS = {"Acoes", "Controle", "Variaveis"};

    static void def(int id, int cat, String label, String[] spec, String[] d) {
        BLABEL[id] = label;
        BCAT[id] = cat;
        BSPEC[id] = spec;
        BDEF[id] = d;
    }

    static String[] sp(String... a) {
        return a;
    }

    static {
        def(B_MSG, 0, "Enviar mensagem ao jogador: {0}", sp("t"), sp("Ola!"));
        def(B_BROADCAST, 0, "Enviar mensagem a todos: {0}", sp("t"), sp("Aviso!"));
        def(B_GIVE, 0, "Dar item {0} quantidade {1}", sp("i", "n"), sp("minecraft:diamond", "1"));
        def(B_HEAL, 0, "Curar {0} coracoes", sp("n"), sp("3"));
        def(B_DAMAGE, 0, "Causar dano de {0} coracoes", sp("n"), sp("1"));
        def(B_FOOD, 0, "Dar {0} de comida", sp("n"), sp("6"));
        def(B_XP, 0, "Dar {0} pontos de XP", sp("n"), sp("10"));
        def(B_POTION, 0, "Efeito {0} por {1} s, nivel {2}", sp("c:potion", "n", "n"), sp("moveSpeed", "30", "1"));
        def(B_TP, 0, "Teleportar para x {0} y {1} z {2}", sp("n", "n", "n"), sp("0", "80", "0"));
        def(B_TPSPAWN, 0, "Ir para o spawn do mundo", sp(), sp());
        def(B_SETBLOCK, 0, "Colocar bloco {0} em ({1},{2},{3}) perto do jogador", sp("i", "n", "n", "n"),
            sp("minecraft:stone", "0", "-1", "0"));
        def(B_AIR, 0, "Remover bloco em ({0},{1},{2}) perto do jogador", sp("n", "n", "n"), sp("0", "-1", "0"));
        def(B_EXPLODE, 0, "Explosao de forca {0}, quebra blocos: {1}", sp("n", "c:yesno"), sp("2", "nao"));
        def(B_LIGHTNING, 0, "Cair um raio no jogador", sp(), sp());
        def(B_SPAWN, 0, "Invocar {0} quantidade {1}", sp("c:mob", "n"), sp("Zombie", "1"));
        def(B_SOUND, 0, "Tocar o som {0}", sp("c:sound"), sp("random.levelup"));
        def(B_TIME, 0, "Mudar o horario para {0}", sp("c:time"), sp("dia"));
        def(B_WEATHER, 0, "Mudar o clima para {0}", sp("c:weather"), sp("limpo"));
        def(B_FIRE, 0, "Pegar fogo por {0} s", sp("n"), sp("5"));
        def(B_LAUNCH, 0, "Lancar o jogador para cima, forca {0}", sp("n"), sp("1.0"));
        def(B_FLY, 0, "Voo do jogador: {0}", sp("c:onoff"), sp("ligado"));
        def(B_SPEED, 0, "Velocidade de andar {0} (normal 0.1)", sp("n"), sp("0.2"));
        def(B_CLEARINV, 0, "Limpar o inventario", sp(), sp());
        def(B_KILL, 0, "Matar mobs em {0} blocos", sp("n"), sp("8"));
        def(B_CANCEL, 0, "Cancelar o evento", sp(), sp());
        def(B_CALL, 0, "Chamar o script {0}", sp("s"), sp(""));
        def(B_WAIT, 0, "Esperar {0} s e depois fazer (ate o Fim)", sp("n"), sp("3"));

        def(B_IF_HEALTH, 1, "Se a vida do jogador {0} {1} coracoes", sp("c:hops", "n"), sp("<", "5"));
        def(B_IF_SNEAK, 1, "Se o jogador esta agachado", sp(), sp());
        def(B_IF_TIME, 1, "Se for {0}", sp("c:daynight"), sp("noite"));
        def(B_IF_RAIN, 1, "Se estiver chovendo", sp(), sp());
        def(B_IF_ITEM, 1, "Se o jogador tem o item {0}", sp("i"), sp("minecraft:diamond"));
        def(B_IF_VAR, 1, "Se a variavel {0} {1} {2}", sp("v", "c:ops", "n"), sp("", ">", "10"));
        def(B_IF_HEIGHT, 1, "Se a altura do jogador {0} {1}", sp("c:hops", "n"), sp(">", "100"));
        def(B_IF_CHANCE, 1, "Se a sorte de {0}% der certo", sp("n"), sp("50"));
        def(B_ELSE, 1, "Senao", sp(), sp());
        def(B_END, 1, "Fim", sp(), sp());
        def(B_REPEAT, 1, "Repetir {0} vezes (ate o Fim)", sp("n"), sp("3"));

        def(B_VSET, 2, "Definir {0} = {1}", sp("v", "n"), sp("", "0"));
        def(B_VADD, 2, "Somar {1} em {0}", sp("v", "n"), sp("", "1"));
        def(B_VSUB, 2, "Subtrair {1} de {0}", sp("v", "n"), sp("", "1"));
        def(B_VMUL, 2, "Multiplicar {0} por {1}", sp("v", "n"), sp("", "2"));
        def(B_VRAND, 2, "{0} = numero entre {1} e {2}", sp("v", "n", "n"), sp("", "1", "10"));
        def(B_VSHOW, 2, "Mostrar {0} no chat", sp("v"), sp(""));
    }

    // listas de escolha
    static final String[] POTIONS = {"moveSpeed", "moveSlowdown", "digSpeed", "digSlowdown", "damageBoost", "heal", "harm", "jump",
        "confusion", "regeneration", "resistance", "fireResistance", "waterBreathing", "invisibility", "blindness", "nightVision",
        "hunger", "weakness", "poison", "wither"};
    static final String[] MOBS = {"Zombie", "Skeleton", "Creeper", "Spider", "CaveSpider", "Pig", "Cow", "Sheep", "Chicken", "Wolf",
        "Enderman", "Slime", "Blaze", "Witch", "Squid", "Bat", "Villager", "LavaSlime", "PigZombie", "VillagerGolem"};
    static final String[] SOUNDS = {"random.levelup", "random.orb", "random.explode", "random.click", "random.bow", "mob.ghast.scream",
        "mob.wither.spawn", "mob.enderdragon.growl", "mob.zombie.say", "mob.cat.meow", "note.harp", "fireworks.launch",
        "mob.horse.zombie.idle", "random.anvil_land", "random.chestopen", "portal.portal"};
    static final String[] TIMES = {"dia", "meio-dia", "por do sol", "noite", "meia-noite"};
    static final int[] TIME_VAL = {1000, 6000, 12000, 14000, 18000};
    static final String[] WEATHER = {"limpo", "chuva", "tempestade"};
    static final String[] OPS = {">", "<", ">=", "<=", "==", "!="};
    static final String[] HOPS = {"<", ">"};
    static final String[] DAYNIGHT = {"noite", "dia"};
    static final String[] YESNO = {"nao", "sim"};
    static final String[] ONOFF = {"ligado", "desligado"};
    static final String[] ITEM_ICONS = {"iron_ingot", "gold_ingot", "diamond", "emerald", "stick", "apple", "coal", "redstone",
        "blaze_rod", "ender_pearl"};
    static final String[] BLOCK_ICONS = {"stone", "dirt", "gold_block", "iron_block", "diamond_block", "glowstone", "obsidian",
        "planks", "brick_block", "netherrack"};

    static String[] choicesOf(String spec) {
        if (spec.equals("c:potion")) return POTIONS;
        if (spec.equals("c:mob")) return MOBS;
        if (spec.equals("c:sound")) return SOUNDS;
        if (spec.equals("c:time")) return TIMES;
        if (spec.equals("c:weather")) return WEATHER;
        if (spec.equals("c:ops")) return OPS;
        if (spec.equals("c:hops")) return HOPS;
        if (spec.equals("c:daynight")) return DAYNIGHT;
        if (spec.equals("c:yesno")) return YESNO;
        if (spec.equals("c:onoff")) return ONOFF;
        if (spec.equals("v")) {
            String[] r = new String[P.vars.size()];
            for (int i = 0; i < r.length; i++) r[i] = P.vars.get(i).name;
            return r;
        }
        if (spec.equals("s")) {
            String[] r = new String[P.scripts.size()];
            for (int i = 0; i < r.length; i++) r[i] = P.scripts.get(i).name;
            return r;
        }
        return new String[0];
    }

    static boolean isTextSpec(String sp) {
        return sp.equals("t") || sp.equals("n") || sp.equals("i");
    }

    static int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i;
        return -1;
    }

    static String choiceOr(String v, String[] list, String def) {
        return indexOf(list, v) >= 0 ? v : def;
    }

    static boolean isIf(int t) {
        return t >= B_IF_HEALTH && t <= B_IF_CHANCE;
    }

    static boolean usesVar(int t) {
        return t == B_IF_VAR || (t >= B_VSET && t <= B_VSHOW);
    }

    // ============================================================================================
    // MODELO DO PROJETO
    // ============================================================================================
    static class VarDef {
        String name = "pontos";
        String init = "0";
    }

    static class ItemDef {
        String id = "item1";
        String display = "Item 1";
        int icon = 0;
        int stack = 64;
        String r1 = "", r2 = "", r3 = "";
    }

    static class BlockDef {
        String id = "bloco1";
        String display = "Bloco 1";
        int base = 0;
        String hard = "2.0";
        String light = "0";
        String r1 = "", r2 = "", r3 = "";
    }

    static class Cmd {
        int type;
        String[] a = {"", "", "", ""};
    }

    static class Script {
        String name = "script1";
        int event = 0;
        String arg = "10";
        final ArrayList<Cmd> cmds = new ArrayList<Cmd>();
    }

    static class Proj {
        String modId = "meumod";
        String name = "Meu Mod";
        String ver = "1.0";
        String pkg = "com.myname.mymodid";
        String cls = "MeuMod";
        final ArrayList<VarDef> vars = new ArrayList<VarDef>();
        final ArrayList<ItemDef> items = new ArrayList<ItemDef>();
        final ArrayList<BlockDef> blocks = new ArrayList<BlockDef>();
        final ArrayList<Script> scripts = new ArrayList<Script>();
    }

    static Proj P = new Proj();
    static volatile boolean openRequested = false;

    static Cmd mk(int type, String... vals) {
        Cmd c = new Cmd();
        c.type = type;
        for (int i = 0; i < 4; i++) c.a[i] = i < BDEF[type].length ? BDEF[type][i] : "";
        for (int i = 0; i < vals.length && i < 4; i++) c.a[i] = vals[i];
        return c;
    }

    static void fixDefaults(Proj p, Cmd c) {
        String[] spec = BSPEC[c.type];
        for (int i = 0; i < spec.length; i++) {
            if (c.a[i].length() > 0) continue;
            if (spec[i].equals("v")) c.a[i] = p.vars.size() > 0 ? p.vars.get(0).name : "pontos";
            else if (spec[i].equals("s") && p.scripts.size() > 0) c.a[i] = p.scripts.get(0).name;
        }
    }

    static Proj sample() {
        Proj p = new Proj();
        VarDef v = new VarDef();
        p.vars.add(v);
        ItemDef it = new ItemDef();
        it.id = "varinha";
        it.display = "Varinha Magica";
        it.icon = 8;
        it.r1 = "minecraft:stick";
        it.r2 = "minecraft:redstone";
        p.items.add(it);
        Script s1 = new Script();
        s1.name = "boasvindas";
        s1.event = EV_JOIN;
        s1.cmds.add(mk(B_MSG, "Bem-vindo ao meu mod!"));
        p.scripts.add(s1);
        Script s2 = new Script();
        s2.name = "raio";
        s2.event = EV_ITEM;
        s2.arg = "varinha";
        s2.cmds.add(mk(B_LIGHTNING));
        s2.cmds.add(mk(B_VADD, "pontos", "1"));
        s2.cmds.add(mk(B_VSHOW, "pontos"));
        p.scripts.add(s2);
        return p;
    }

    static Proj emptyProject(String modId) {
        Proj p = new Proj();
        p.modId = modId;
        p.name = "Novo Mod";
        p.cls = "NovoMod";
        return p;
    }

    // ============================================================================================
    // CICLO DE VIDA
    // ============================================================================================
    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientCommandHandler.instance.registerCommand(new CmdMaker());
            FMLCommonHandler.instance().bus().register(new ClientTick());
            try {
                String last = readLast();
                Proj lp = last != null ? loadProject(last) : null;
                P = lp != null ? lp : sample();
            } catch (Exception ex) {
                P = sample();
            }
        }
    }

    public static class CmdMaker extends CommandBase {
        @Override
        public String getCommandName() {
            return "modmaker";
        }

        @Override
        public List getCommandAliases() {
            return Arrays.asList("mm");
        }

        @Override
        public String getCommandUsage(ICommandSender s) {
            return "/modmaker";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender s) {
            return true;
        }

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            openRequested = true;
        }
    }

    public static class ClientTick {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END || !openRequested) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null && mc.currentScreen == null) {
                openRequested = false;
                mc.displayGuiScreen(new GuiMaker());
            }
        }
    }

    // ============================================================================================
    // SALVAR E CARREGAR PROJETOS
    // ============================================================================================
    static File dir() {
        File d = new File(Minecraft.getMinecraft().mcDataDir, "modmaker");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    static File pdir() {
        File d = new File(dir(), "projetos");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    static String enc(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
    }

    static String dec(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 'p' ? '|' : (n == 'n' ? '\n' : n));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    static String projFileName(Proj p) {
        return lowerId(p.modId, "meumod") + ".txt";
    }

    static String readLast() {
        try {
            File f = new File(dir(), "ultimo.txt");
            if (!f.exists()) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            try {
                return r.readLine();
            } finally {
                r.close();
            }
        } catch (Exception ex) {
            return null;
        }
    }

    static void saveProject(Proj p) throws Exception {
        File f = new File(pdir(), projFileName(p));
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
        try {
            w.write("P|" + enc(p.modId) + "|" + enc(p.name) + "|" + enc(p.ver) + "|" + enc(p.pkg) + "|" + enc(p.cls) + "\n");
            for (VarDef v : p.vars) w.write("V|" + enc(v.name) + "|" + enc(v.init) + "\n");
            for (ItemDef i : p.items) {
                w.write("I|" + enc(i.id) + "|" + enc(i.display) + "|" + i.icon + "|" + i.stack + "|" + enc(i.r1) + "|" + enc(i.r2)
                    + "|" + enc(i.r3) + "\n");
            }
            for (BlockDef b : p.blocks) {
                w.write("B|" + enc(b.id) + "|" + enc(b.display) + "|" + b.base + "|" + enc(b.hard) + "|" + enc(b.light) + "|"
                    + enc(b.r1) + "|" + enc(b.r2) + "|" + enc(b.r3) + "\n");
            }
            for (Script s : p.scripts) {
                w.write("S|" + enc(s.name) + "|" + s.event + "|" + enc(s.arg) + "\n");
                for (Cmd c : s.cmds) {
                    w.write("C|" + c.type + "|" + enc(c.a[0]) + "|" + enc(c.a[1]) + "|" + enc(c.a[2]) + "|" + enc(c.a[3]) + "\n");
                }
            }
        } finally {
            w.close();
        }
        BufferedWriter l = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dir(), "ultimo.txt")), "UTF-8"));
        try {
            l.write(lowerId(p.modId, "meumod") + "\n");
        } finally {
            l.close();
        }
    }

    static int toInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ex) {
            return def;
        }
    }

    static Proj loadProject(String modId) throws Exception {
        File f = new File(pdir(), lowerId(modId, "meumod") + ".txt");
        if (!f.exists()) return null;
        Proj p = new Proj();
        Script cur = null;
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                String[] t = line.split("\\|", -1);
                if (t.length < 2) continue;
                if (t[0].equals("P") && t.length >= 6) {
                    p.modId = dec(t[1]);
                    p.name = dec(t[2]);
                    p.ver = dec(t[3]);
                    p.pkg = dec(t[4]);
                    p.cls = dec(t[5]);
                } else if (t[0].equals("V") && t.length >= 3) {
                    VarDef v = new VarDef();
                    v.name = dec(t[1]);
                    v.init = dec(t[2]);
                    p.vars.add(v);
                } else if (t[0].equals("I") && t.length >= 8) {
                    ItemDef i = new ItemDef();
                    i.id = dec(t[1]);
                    i.display = dec(t[2]);
                    i.icon = Math.max(0, Math.min(ITEM_ICONS.length - 1, toInt(t[3], 0)));
                    i.stack = toInt(t[4], 64);
                    i.r1 = dec(t[5]);
                    i.r2 = dec(t[6]);
                    i.r3 = dec(t[7]);
                    p.items.add(i);
                } else if (t[0].equals("B") && t.length >= 9) {
                    BlockDef b = new BlockDef();
                    b.id = dec(t[1]);
                    b.display = dec(t[2]);
                    b.base = Math.max(0, Math.min(BLOCK_ICONS.length - 1, toInt(t[3], 0)));
                    b.hard = dec(t[4]);
                    b.light = dec(t[5]);
                    b.r1 = dec(t[6]);
                    b.r2 = dec(t[7]);
                    b.r3 = dec(t[8]);
                    p.blocks.add(b);
                } else if (t[0].equals("S") && t.length >= 4) {
                    cur = new Script();
                    cur.name = dec(t[1]);
                    cur.event = Math.max(0, Math.min(EV_NAME.length - 1, toInt(t[2], 0)));
                    cur.arg = dec(t[3]);
                    p.scripts.add(cur);
                } else if (t[0].equals("C") && t.length >= 6 && cur != null) {
                    int type = toInt(t[1], -1);
                    if (type < 0 || type >= NB) continue;
                    Cmd c = new Cmd();
                    c.type = type;
                    for (int k = 0; k < 4; k++) c.a[k] = dec(t[2 + k]);
                    cur.cmds.add(c);
                }
            }
        } finally {
            r.close();
        }
        return p;
    }

    static ArrayList<String> listProjects() {
        ArrayList<String> r = new ArrayList<String>();
        File[] fs = pdir().listFiles();
        if (fs != null) {
            for (File f : fs) {
                String n = f.getName();
                if (n.endsWith(".txt")) r.add(n.substring(0, n.length() - 4));
            }
        }
        return r;
    }

    // ============================================================================================
    // AJUDANTES DE TEXTO E NUMERO
    // ============================================================================================
    static String lowerId(String s, String def) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') b.append(c);
        }
        if (b.length() == 0) return def;
        if (b.charAt(0) >= '0' && b.charAt(0) <= '9') b.insert(0, 'x');
        return b.toString();
    }

    static String className(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') b.append(c);
        }
        if (b.length() == 0) return "MeuMod";
        char f = b.charAt(0);
        if (f >= '0' && f <= '9') b.insert(0, 'M');
        else b.setCharAt(0, Character.toUpperCase(f));
        return b.toString();
    }

    static String pkgName(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.') b.append(c);
        }
        String r = b.toString();
        while (r.indexOf("..") >= 0) r = r.replace("..", ".");
        while (r.startsWith(".")) r = r.substring(1);
        while (r.endsWith(".")) r = r.substring(0, r.length() - 1);
        return r.length() == 0 ? "com.myname.mymodid" : r;
    }

    static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') b.append("\\\\");
            else if (c == '"') b.append("\\\"");
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("");
            else if (c < 32 || c > 126) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.toString();
    }

    static String safe(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 32 && c <= 126 && c != '\\' ? c : '?');
        }
        return b.toString();
    }

    static double parse(String s, double def) {
        try {
            double d = Double.parseDouble(s.trim().replace(',', '.'));
            if (Double.isNaN(d) || Double.isInfinite(d)) return def;
            return d;
        } catch (Exception ex) {
            return def;
        }
    }

    static String D(String s, double def) {
        return String.valueOf(parse(s, def));
    }

    static String I(String s, int def) {
        return String.valueOf((int) Math.round(parse(s, def)));
    }

    static String F(String s, double def) {
        return D(s, def) + "F";
    }

    static String uniq(HashSet<String> used, String base) {
        String b = base;
        int i = 2;
        while (used.contains(b)) b = base + (i++);
        used.add(b);
        return b;
    }

    static String pad(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append("    ");
        return b.toString();
    }

    static void L(StringBuilder o, String s) {
        o.append(s).append('\n');
    }

    static void addVar(ArrayList<String> names, ArrayList<String> inits, String n, String init) {
        if (names.contains(n)) return;
        names.add(n);
        inits.add(init);
    }

    static int[] indents(ArrayList<Cmd> cmds) {
        int[] r = new int[cmds.size()];
        ArrayList<Character> st = new ArrayList<Character>();
        for (int i = 0; i < r.length; i++) {
            int t = cmds.get(i).type;
            if (t == B_END) {
                while (!st.isEmpty() && st.get(st.size() - 1) == 'w') st.remove(st.size() - 1);
                if (!st.isEmpty()) st.remove(st.size() - 1);
                r[i] = st.size();
            } else if (t == B_ELSE) {
                while (!st.isEmpty() && st.get(st.size() - 1) == 'w') st.remove(st.size() - 1);
                r[i] = Math.max(0, st.size() - 1);
                if (!st.isEmpty() && st.get(st.size() - 1) == 'i') st.set(st.size() - 1, 'e');
            } else {
                r[i] = st.size();
                if (isIf(t)) st.add('i');
                else if (t == B_REPEAT) st.add('r');
                else if (t == B_WAIT) st.add('w');
            }
        }
        return r;
    }

    static String labelText(Cmd c) {
        String t = BLABEL[c.type];
        for (int i = 0; i < 4; i++) t = t.replace("{" + i + "}", "[" + c.a[i] + "]");
        return t;
    }

    // ============================================================================================
    // GERADOR DE CODIGO
    // ============================================================================================
    static String generate(Proj p, ArrayList<String> warns) {
        warns.clear();
        StringBuilder o = new StringBuilder();
        String cls = className(p.cls);
        String modid = lowerId(p.modId, "meumod");
        String pkg = pkgName(p.pkg);

        HashSet<String> used = new HashSet<String>();
        String[] itemIds = new String[p.items.size()];
        String[] blockIds = new String[p.blocks.size()];
        for (int i = 0; i < itemIds.length; i++) itemIds[i] = uniq(used, lowerId(p.items.get(i).id, "item"));
        for (int i = 0; i < blockIds.length; i++) blockIds[i] = uniq(used, lowerId(p.blocks.get(i).id, "bloco"));

        ArrayList<String> vn = new ArrayList<String>();
        ArrayList<String> vi = new ArrayList<String>();
        for (VarDef v : p.vars) addVar(vn, vi, lowerId(v.name, "v"), D(v.init, 0));
        for (Script s : p.scripts) {
            for (Cmd c : s.cmds) if (usesVar(c.type)) addVar(vn, vi, lowerId(c.a[0], "v"), "0.0");
        }

        L(o, "// Gerado pelo Criador de Mod (modmaker). Compile no GitHub com o template do Forge 1.7.10.");
        L(o, "// Salve este arquivo em: src/main/java/" + pkg.replace('.', '/') + "/" + cls + ".java");
        L(o, "// O modid \"" + modid + "\" nao pode ser igual ao de outro mod no mesmo jar.");
        L(o, "");
        L(o, "package " + pkg + ";");
        L(o, "");
        String[] imports = {"java.util.ArrayList", "java.util.List", "java.util.Random", "cpw.mods.fml.common.FMLCommonHandler",
            "cpw.mods.fml.common.Mod", "cpw.mods.fml.common.event.FMLInitializationEvent",
            "cpw.mods.fml.common.event.FMLPreInitializationEvent", "cpw.mods.fml.common.event.FMLServerStartingEvent",
            "cpw.mods.fml.common.eventhandler.Event", "cpw.mods.fml.common.eventhandler.SubscribeEvent",
            "cpw.mods.fml.common.gameevent.PlayerEvent", "cpw.mods.fml.common.gameevent.TickEvent",
            "cpw.mods.fml.common.registry.GameRegistry", "cpw.mods.fml.common.registry.LanguageRegistry",
            "net.minecraft.block.Block", "net.minecraft.block.material.Material",
            "net.minecraft.client.renderer.texture.IIconRegister", "net.minecraft.command.CommandBase",
            "net.minecraft.command.ICommandSender", "net.minecraft.creativetab.CreativeTabs", "net.minecraft.entity.Entity",
            "net.minecraft.entity.EntityList", "net.minecraft.entity.EntityLiving",
            "net.minecraft.entity.effect.EntityLightningBolt", "net.minecraft.entity.player.EntityPlayer",
            "net.minecraft.init.Blocks", "net.minecraft.init.Items", "net.minecraft.item.Item", "net.minecraft.item.ItemStack",
            "net.minecraft.potion.Potion", "net.minecraft.potion.PotionEffect", "net.minecraft.server.MinecraftServer",
            "net.minecraft.util.ChatComponentText", "net.minecraft.util.ChunkCoordinates", "net.minecraft.util.DamageSource",
            "net.minecraft.util.IIcon", "net.minecraft.world.World", "net.minecraftforge.common.MinecraftForge",
            "net.minecraftforge.event.entity.living.LivingDeathEvent", "net.minecraftforge.event.entity.living.LivingEvent",
            "net.minecraftforge.event.entity.living.LivingHurtEvent", "net.minecraftforge.event.world.BlockEvent"};
        for (String im : imports) L(o, "import " + im + ";");
        L(o, "");
        L(o, "@Mod(modid = " + cls + ".MODID, name = \"" + esc(p.name) + "\", version = \"" + esc(p.ver)
            + "\", acceptedMinecraftVersions = \"[1.7.10]\")");
        L(o, "public class " + cls + " {");
        L(o, "");
        L(o, "    public static final String MODID = \"" + modid + "\";");
        L(o, "    static final Random RND = new Random();");
        L(o, "    static int tickCounter = 0;");
        L(o, "    static int callDepth = 0;");
        L(o, "");
        L(o, "    // ---------- variaveis ----------");
        for (int i = 0; i < vn.size(); i++) L(o, "    static double v_" + vn.get(i) + " = " + vi.get(i) + ";");
        L(o, "");
        L(o, "    // ---------- itens e blocos ----------");
        for (int i = 0; i < itemIds.length; i++) L(o, "    public static Item item_" + i + ";");
        for (int i = 0; i < blockIds.length; i++) L(o, "    public static Block block_" + i + ";");
        L(o, "");

        // ciclo de vida
        L(o, "    @Mod.EventHandler");
        L(o, "    public void preInit(FMLPreInitializationEvent e) {");
        for (int i = 0; i < itemIds.length; i++) {
            ItemDef it = p.items.get(i);
            L(o, "        item_" + i + " = new GenItem(" + i + ", \"" + itemIds[i] + "\", " + Math.max(1, Math.min(64, it.stack)) + ", "
                + Math.max(0, Math.min(ITEM_ICONS.length - 1, it.icon)) + ");");
            L(o, "        GameRegistry.registerItem(item_" + i + ", \"" + itemIds[i] + "\");");
        }
        for (int i = 0; i < blockIds.length; i++) {
            BlockDef b = p.blocks.get(i);
            int light = (int) Math.max(0, Math.min(15, Math.round(parse(b.light, 0))));
            L(o, "        block_" + i + " = new GenBlock(" + i + ", \"" + blockIds[i] + "\", " + F(b.hard, 2.0) + ", " + light + ", "
                + Math.max(0, Math.min(BLOCK_ICONS.length - 1, b.base)) + ");");
            L(o, "        GameRegistry.registerBlock(block_" + i + ", \"" + blockIds[i] + "\");");
        }
        L(o, "    }");
        L(o, "");
        L(o, "    @Mod.EventHandler");
        L(o, "    public void init(FMLInitializationEvent e) {");
        L(o, "        LanguageRegistry lang = LanguageRegistry.instance();");
        for (int i = 0; i < itemIds.length; i++) {
            ItemDef it = p.items.get(i);
            L(o, "        lang.addStringLocalization(\"item." + itemIds[i] + ".name\", \"" + esc(it.display) + "\");");
            if (it.r1.length() + it.r2.length() + it.r3.length() > 0) {
                L(o, "        recipe(new ItemStack(item_" + i + ", 1), \"" + esc(it.r1) + "\", \"" + esc(it.r2) + "\", \"" + esc(it.r3)
                    + "\");");
            }
        }
        for (int i = 0; i < blockIds.length; i++) {
            BlockDef b = p.blocks.get(i);
            L(o, "        lang.addStringLocalization(\"tile." + blockIds[i] + ".name\", \"" + esc(b.display) + "\");");
            if (b.r1.length() + b.r2.length() + b.r3.length() > 0) {
                L(o, "        recipe(new ItemStack(block_" + i + ", 1), \"" + esc(b.r1) + "\", \"" + esc(b.r2) + "\", \"" + esc(b.r3)
                    + "\");");
            }
        }
        L(o, "        Events ev = new Events();");
        L(o, "        MinecraftForge.EVENT_BUS.register(ev);");
        L(o, "        FMLCommonHandler.instance().bus().register(ev);");
        L(o, "    }");
        L(o, "");
        L(o, "    @Mod.EventHandler");
        L(o, "    public void serverStarting(FMLServerStartingEvent e) {");
        HashSet<String> cmdNames = new HashSet<String>();
        for (int k = 0; k < p.scripts.size(); k++) {
            Script s = p.scripts.get(k);
            if (s.event != EV_CMD) continue;
            String cn = lowerId(s.arg, "comando");
            if (!cmdNames.add(cn)) {
                warns.add("Comando /" + cn + " repetido em mais de um script.");
                continue;
            }
            L(o, "        e.registerServerCommand(new ScriptCommand(\"" + cn + "\", " + k + "));");
        }
        L(o, "    }");
        L(o, "");

        // ajudantes
        L(o, "    // ---------- ajudantes ----------");
        L(o, "    static void msg(EntityPlayer p, String s) {");
        L(o, "        p.addChatMessage(new ChatComponentText(s));");
        L(o, "    }");
        L(o, "");
        L(o, "    static void broadcast(World w, String s) {");
        L(o, "        for (Object o : w.playerEntities) ((EntityPlayer) o).addChatMessage(new ChatComponentText(s));");
        L(o, "    }");
        L(o, "");
        L(o, "    static int bx(EntityPlayer p) {");
        L(o, "        return (int) Math.floor(p.posX);");
        L(o, "    }");
        L(o, "");
        L(o, "    static int by(EntityPlayer p) {");
        L(o, "        return (int) Math.floor(p.posY);");
        L(o, "    }");
        L(o, "");
        L(o, "    static int bz(EntityPlayer p) {");
        L(o, "        return (int) Math.floor(p.posZ);");
        L(o, "    }");
        L(o, "");
        L(o, "    static ItemStack stackOf(String name, int n) {");
        L(o, "        int i = name.indexOf(':');");
        L(o, "        String mod = i > 0 ? name.substring(0, i) : \"minecraft\";");
        L(o, "        String id = i > 0 ? name.substring(i + 1) : name;");
        L(o, "        Item it = GameRegistry.findItem(mod, id);");
        L(o, "        if (it != null) return new ItemStack(it, n);");
        L(o, "        Block b = GameRegistry.findBlock(mod, id);");
        L(o, "        if (b != null) return new ItemStack(b, n);");
        L(o, "        return null;");
        L(o, "    }");
        L(o, "");
        L(o, "    static void giveItem(EntityPlayer p, String name, int n) {");
        L(o, "        ItemStack st = stackOf(name, Math.max(1, n));");
        L(o, "        if (st == null) {");
        L(o, "            msg(p, \"Item nao encontrado: \" + name);");
        L(o, "            return;");
        L(o, "        }");
        L(o, "        if (!p.inventory.addItemStackToInventory(st)) p.dropPlayerItemWithRandomChoice(st, false);");
        L(o, "    }");
        L(o, "");
        L(o, "    static boolean hasItem(EntityPlayer p, String name) {");
        L(o, "        ItemStack t = stackOf(name, 1);");
        L(o, "        if (t == null) return false;");
        L(o, "        for (ItemStack s : p.inventory.mainInventory) {");
        L(o, "            if (s != null && s.getItem() == t.getItem()) return true;");
        L(o, "        }");
        L(o, "        return false;");
        L(o, "    }");
        L(o, "");
        L(o, "    static void setBlockAt(World w, int x, int y, int z, String name) {");
        L(o, "        int i = name.indexOf(':');");
        L(o, "        String mod = i > 0 ? name.substring(0, i) : \"minecraft\";");
        L(o, "        String id = i > 0 ? name.substring(i + 1) : name;");
        L(o, "        Block b = GameRegistry.findBlock(mod, id);");
        L(o, "        if (b != null) w.setBlock(x, y, z, b);");
        L(o, "    }");
        L(o, "");
        L(o, "    static void spawnMob(World w, String mob, double x, double y, double z, int n) {");
        L(o, "        for (int k = 0; k < n; k++) {");
        L(o, "            Entity e = EntityList.createEntityByName(mob, w);");
        L(o, "            if (e != null) {");
        L(o, "                e.setLocationAndAngles(x + RND.nextDouble() * 2.0D - 1.0D, y, z + RND.nextDouble() * 2.0D - 1.0D, RND.nextFloat() * 360.0F, 0.0F);");
        L(o, "                w.spawnEntityInWorld(e);");
        L(o, "            }");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static void killAround(EntityPlayer p, double r) {");
        L(o, "        List l = p.worldObj.getEntitiesWithinAABB(EntityLiving.class, p.boundingBox.expand(r, r, r));");
        L(o, "        for (Object o : l) ((Entity) o).setDead();");
        L(o, "    }");
        L(o, "");
        L(o, "    static class Task {");
        L(o, "        int ticks;");
        L(o, "        Runnable run;");
        L(o, "");
        L(o, "        Task(int t, Runnable r) {");
        L(o, "            ticks = t;");
        L(o, "            run = r;");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static final ArrayList<Task> TASKS = new ArrayList<Task>();");
        L(o, "");
        L(o, "    static void schedule(int ticks, Runnable r) {");
        L(o, "        TASKS.add(new Task(Math.max(1, ticks), r));");
        L(o, "    }");
        L(o, "");
        L(o, "    static void runTasks() {");
        L(o, "        ArrayList<Task> copy = new ArrayList<Task>(TASKS);");
        L(o, "        for (Task t : copy) {");
        L(o, "            if (--t.ticks <= 0) {");
        L(o, "                TASKS.remove(t);");
        L(o, "                try {");
        L(o, "                    t.run.run();");
        L(o, "                } catch (Throwable ex) {");
        L(o, "                    // ignora erro do script");
        L(o, "                }");
        L(o, "            }");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static void runForAll(int k) {");
        L(o, "        MinecraftServer srv = MinecraftServer.getServer();");
        L(o, "        if (srv == null) return;");
        L(o, "        List l = new ArrayList(srv.getConfigurationManager().playerEntityList);");
        L(o, "        for (Object o : l) {");
        L(o, "            EntityPlayer p = (EntityPlayer) o;");
        L(o, "            callScript(k, p, p.worldObj, null);");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static void recipe(ItemStack out, String a, String b, String c) {");
        L(o, "        ArrayList<Object> in = new ArrayList<Object>();");
        L(o, "        String[] arr = {a, b, c};");
        L(o, "        for (String s : arr) {");
        L(o, "            if (s == null || s.length() == 0) continue;");
        L(o, "            ItemStack st = stackOf(s, 1);");
        L(o, "            if (st == null) return;");
        L(o, "            in.add(st);");
        L(o, "        }");
        L(o, "        if (!in.isEmpty()) GameRegistry.addShapelessRecipe(out, in.toArray());");
        L(o, "    }");
        L(o, "");
        L(o, "    static IIcon iconOfItem(int i) {");
        L(o, "        switch (i) {");
        for (int i = 1; i < ITEM_ICONS.length; i++) L(o, "            case " + i + ": return Items." + ITEM_ICONS[i] + ".getIconFromDamage(0);");
        L(o, "            default: return Items." + ITEM_ICONS[0] + ".getIconFromDamage(0);");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static IIcon iconOfBlock(int i) {");
        L(o, "        switch (i) {");
        for (int i = 1; i < BLOCK_ICONS.length; i++) L(o, "            case " + i + ": return Blocks." + BLOCK_ICONS[i] + ".getIcon(0, 0);");
        L(o, "            default: return Blocks." + BLOCK_ICONS[0] + ".getIcon(0, 0);");
        L(o, "        }");
        L(o, "    }");
        L(o, "");

        // despachos
        L(o, "    static void callScript(int k, EntityPlayer p, World w, Event ev) {");
        L(o, "        if (callDepth > 12) return;");
        L(o, "        callDepth++;");
        L(o, "        try {");
        L(o, "            switch (k) {");
        for (int k = 0; k < p.scripts.size(); k++) L(o, "                case " + k + ": script_" + k + "(p, w, ev); break;");
        L(o, "                default: break;");
        L(o, "            }");
        L(o, "        } finally {");
        L(o, "            callDepth--;");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static void itemUse(int index, World w, EntityPlayer p) {");
        L(o, "        switch (index) {");
        for (int i = 0; i < itemIds.length; i++) {
            ArrayList<Integer> ks = scriptsFor(p, EV_ITEM, p.items.get(i).id);
            if (ks.isEmpty()) continue;
            L(o, "            case " + i + ":");
            for (int k : ks) L(o, "                callScript(" + k + ", p, w, null);");
            L(o, "                break;");
        }
        L(o, "            default: break;");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static boolean blockHas(int index) {");
        L(o, "        switch (index) {");
        for (int i = 0; i < blockIds.length; i++) {
            if (!scriptsFor(p, EV_BLOCK, p.blocks.get(i).id).isEmpty()) L(o, "            case " + i + ": return true;");
        }
        L(o, "            default: return false;");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    static void blockUse(int index, World w, EntityPlayer p) {");
        L(o, "        switch (index) {");
        for (int i = 0; i < blockIds.length; i++) {
            ArrayList<Integer> ks = scriptsFor(p, EV_BLOCK, p.blocks.get(i).id);
            if (ks.isEmpty()) continue;
            L(o, "            case " + i + ":");
            for (int k : ks) L(o, "                callScript(" + k + ", p, w, null);");
            L(o, "                break;");
        }
        L(o, "            default: break;");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        for (Script s : p.scripts) {
            if (s.event == EV_ITEM && indexOfId(p.items, s.arg) < 0) warns.add("Script " + s.name + ": o item \"" + s.arg + "\" nao existe.");
            if (s.event == EV_BLOCK && indexOfBlockId(p.blocks, s.arg) < 0) warns.add("Script " + s.name + ": o bloco \"" + s.arg + "\" nao existe.");
        }

        // classes de item e bloco
        L(o, "    public static class GenItem extends Item {");
        L(o, "        final int index;");
        L(o, "        final int icon;");
        L(o, "");
        L(o, "        public GenItem(int index, String name, int maxStack, int icon) {");
        L(o, "            this.index = index;");
        L(o, "            this.icon = icon;");
        L(o, "            setUnlocalizedName(name);");
        L(o, "            setMaxStackSize(maxStack);");
        L(o, "            setCreativeTab(CreativeTabs.tabMisc);");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public void registerIcons(IIconRegister r) {}");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public IIcon getIconFromDamage(int d) {");
        L(o, "            return iconOfItem(icon);");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public ItemStack onItemRightClick(ItemStack s, World w, EntityPlayer p) {");
        L(o, "            if (!w.isRemote) itemUse(index, w, p);");
        L(o, "            return s;");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    public static class GenBlock extends Block {");
        L(o, "        final int index;");
        L(o, "        final int base;");
        L(o, "");
        L(o, "        public GenBlock(int index, String name, float hard, int light, int base) {");
        L(o, "            super(Material.rock);");
        L(o, "            this.index = index;");
        L(o, "            this.base = base;");
        L(o, "            setBlockName(name);");
        L(o, "            setHardness(hard);");
        L(o, "            setResistance(hard * 5.0F);");
        L(o, "            setLightLevel(light / 15.0F);");
        L(o, "            setCreativeTab(CreativeTabs.tabBlock);");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public void registerBlockIcons(IIconRegister r) {}");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public IIcon getIcon(int side, int meta) {");
        L(o, "            return iconOfBlock(base);");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {");
        L(o, "            if (!w.isRemote) blockUse(index, w, p);");
        L(o, "            return blockHas(index);");
        L(o, "        }");
        L(o, "    }");
        L(o, "");
        L(o, "    public static class ScriptCommand extends CommandBase {");
        L(o, "        final String cmd;");
        L(o, "        final int script;");
        L(o, "");
        L(o, "        public ScriptCommand(String cmd, int script) {");
        L(o, "            this.cmd = cmd;");
        L(o, "            this.script = script;");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public String getCommandName() {");
        L(o, "            return cmd;");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public String getCommandUsage(ICommandSender s) {");
        L(o, "            return \"/\" + cmd;");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public int getRequiredPermissionLevel() {");
        L(o, "            return 0;");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public boolean canCommandSenderUseCommand(ICommandSender s) {");
        L(o, "            return true;");
        L(o, "        }");
        L(o, "");
        L(o, "        @Override");
        L(o, "        public void processCommand(ICommandSender s, String[] a) {");
        L(o, "            if (s instanceof EntityPlayer) callScript(script, (EntityPlayer) s, ((EntityPlayer) s).worldObj, null);");
        L(o, "        }");
        L(o, "    }");
        L(o, "");

        // eventos
        L(o, "    public static class Events {");
        L(o, "        @SubscribeEvent");
        L(o, "        public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {");
        L(o, "            if (e.player.worldObj.isRemote) return;");
        callsFor(o, p, EV_JOIN, "e.player", "e.player.worldObj", "null");
        L(o, "        }");
        L(o, "");
        L(o, "        @SubscribeEvent");
        L(o, "        public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {");
        L(o, "            if (e.player.worldObj.isRemote) return;");
        callsFor(o, p, EV_RESPAWN, "e.player", "e.player.worldObj", "null");
        L(o, "        }");
        L(o, "");
        L(o, "        @SubscribeEvent");
        L(o, "        public void onTick(TickEvent.ServerTickEvent e) {");
        L(o, "            if (e.phase != TickEvent.Phase.END) return;");
        L(o, "            tickCounter++;");
        L(o, "            runTasks();");
        for (int k = 0; k < p.scripts.size(); k++) {
            Script s = p.scripts.get(k);
            if (s.event == EV_TIME) {
                int n = (int) Math.max(1, Math.round(parse(s.arg, 10) * 20.0));
                L(o, "            if (tickCounter % " + n + " == 0) runForAll(" + k + ");");
            }
        }
        L(o, "        }");
        L(o, "");
        L(o, "        @SubscribeEvent");
        L(o, "        public void onBreak(BlockEvent.BreakEvent e) {");
        L(o, "            if (e.world.isRemote || e.getPlayer() == null) return;");
        callsFor(o, p, EV_BREAK, "e.getPlayer()", "e.world", "e");
        L(o, "        }");
        L(o, "");
        L(o, "        @SubscribeEvent");
        L(o, "        public void onDeath(LivingDeathEvent e) {");
        L(o, "            if (e.entityLiving.worldObj.isRemote) return;");
        L(o, "            if (e.entityLiving instanceof EntityPlayer) {");
        L(o, "                EntityPlayer pl = (EntityPlayer) e.entityLiving;");
        callsFor(o, p, EV_DIE, "pl", "pl.worldObj", "e");
        L(o, "            } else if (e.source != null && e.source.getEntity() instanceof EntityPlayer) {");
        L(o, "                EntityPlayer pl = (EntityPlayer) e.source.getEntity();");
        callsFor(o, p, EV_KILL, "pl", "pl.worldObj", "e");
        L(o, "            }");
        L(o, "        }");
        L(o, "");
        L(o, "        @SubscribeEvent");
        L(o, "        public void onHurt(LivingHurtEvent e) {");
        L(o, "            if (e.entityLiving.worldObj.isRemote || !(e.entityLiving instanceof EntityPlayer)) return;");
        L(o, "            EntityPlayer pl = (EntityPlayer) e.entityLiving;");
        callsFor(o, p, EV_HURT, "pl", "pl.worldObj", "e");
        L(o, "        }");
        L(o, "");
        L(o, "        @SubscribeEvent");
        L(o, "        public void onJump(LivingEvent.LivingJumpEvent e) {");
        L(o, "            if (e.entityLiving.worldObj.isRemote || !(e.entityLiving instanceof EntityPlayer)) return;");
        L(o, "            EntityPlayer pl = (EntityPlayer) e.entityLiving;");
        callsFor(o, p, EV_JUMP, "pl", "pl.worldObj", "null");
        L(o, "        }");
        L(o, "    }");
        L(o, "");

        // scripts
        L(o, "    // ---------- scripts ----------");
        for (int k = 0; k < p.scripts.size(); k++) {
            Script s = p.scripts.get(k);
            L(o, "    // Script: " + safe(s.name) + " | evento: " + EV_NAME[s.event]);
            L(o, "    static void script_" + k + "(final EntityPlayer player, final World world, final Event ev) {");
            genBody(o, p, s, warns);
            L(o, "    }");
            L(o, "");
        }
        L(o, "}");
        return o.toString();
    }

    static int indexOfId(ArrayList<ItemDef> l, String id) {
        for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(id)) return i;
        return -1;
    }

    static int indexOfBlockId(ArrayList<BlockDef> l, String id) {
        for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(id)) return i;
        return -1;
    }

    static ArrayList<Integer> scriptsFor(Proj p, int ev, String arg) {
        ArrayList<Integer> r = new ArrayList<Integer>();
        for (int k = 0; k < p.scripts.size(); k++) {
            Script s = p.scripts.get(k);
            if (s.event == ev && s.arg.equals(arg)) r.add(k);
        }
        return r;
    }

    static void callsFor(StringBuilder o, Proj p, int ev, String player, String world, String evt) {
        for (int k = 0; k < p.scripts.size(); k++) {
            if (p.scripts.get(k).event == ev) L(o, "            callScript(" + k + ", " + player + ", " + world + ", " + evt + ");");
        }
    }

    static void genBody(StringBuilder o, Proj p, Script s, ArrayList<String> warns) {
        ArrayList<Character> st = new ArrayList<Character>();
        int loops = 0;
        for (Cmd c : s.cmds) {
            String pd = pad(2 + st.size());
            String a0 = c.a[0];
            String a1 = c.a[1];
            String a2 = c.a[2];
            String a3 = c.a[3];
            String v = "v_" + lowerId(a0, "v");
            switch (c.type) {
                case B_MSG:
                    L(o, pd + "msg(player, \"" + esc(a0) + "\");");
                    break;
                case B_BROADCAST:
                    L(o, pd + "broadcast(world, \"" + esc(a0) + "\");");
                    break;
                case B_GIVE:
                    L(o, pd + "giveItem(player, \"" + esc(a0) + "\", " + I(a1, 1) + ");");
                    break;
                case B_HEAL:
                    L(o, pd + "player.heal((float) (" + D(a0, 3) + " * 2.0D));");
                    break;
                case B_DAMAGE:
                    L(o, pd + "player.attackEntityFrom(DamageSource.generic, (float) (" + D(a0, 1) + " * 2.0D));");
                    break;
                case B_FOOD:
                    L(o, pd + "player.getFoodStats().addStats(" + I(a0, 4) + ", 1.0F);");
                    break;
                case B_XP:
                    L(o, pd + "player.addExperience(" + I(a0, 10) + ");");
                    break;
                case B_POTION:
                    L(o, pd + "player.addPotionEffect(new PotionEffect(Potion." + choiceOr(a0, POTIONS, "moveSpeed") + ".id, (int) ("
                        + D(a1, 30) + " * 20.0D), " + I(a2, 0) + "));");
                    break;
                case B_TP:
                    L(o, pd + "player.setPositionAndUpdate(" + D(a0, 0) + ", " + D(a1, 80) + ", " + D(a2, 0) + ");");
                    break;
                case B_TPSPAWN:
                    L(o, pd + "{");
                    L(o, pd + "    ChunkCoordinates cc = world.getSpawnPoint();");
                    L(o, pd + "    player.setPositionAndUpdate(cc.posX + 0.5D, cc.posY, cc.posZ + 0.5D);");
                    L(o, pd + "}");
                    break;
                case B_SETBLOCK:
                    L(o, pd + "setBlockAt(world, bx(player) + " + I(a1, 0) + ", by(player) + " + I(a2, -1) + ", bz(player) + " + I(a3, 0)
                        + ", \"" + esc(a0) + "\");");
                    break;
                case B_AIR:
                    L(o, pd + "world.setBlockToAir(bx(player) + " + I(a0, 0) + ", by(player) + " + I(a1, -1) + ", bz(player) + " + I(a2, 0)
                        + ");");
                    break;
                case B_EXPLODE:
                    L(o, pd + "world.createExplosion(null, player.posX, player.posY, player.posZ, " + F(a0, 2) + ", "
                        + (a1.equals("sim") ? "true" : "false") + ");");
                    break;
                case B_LIGHTNING:
                    L(o, pd + "world.addWeatherEffect(new EntityLightningBolt(world, player.posX, player.posY, player.posZ));");
                    break;
                case B_SPAWN:
                    L(o, pd + "spawnMob(world, \"" + choiceOr(a0, MOBS, "Zombie") + "\", player.posX, player.posY, player.posZ, "
                        + I(a1, 1) + ");");
                    break;
                case B_SOUND:
                    L(o, pd + "world.playSoundAtEntity(player, \"" + choiceOr(a0, SOUNDS, SOUNDS[0]) + "\", 1.0F, 1.0F);");
                    break;
                case B_TIME: {
                    int ti = Math.max(0, indexOf(TIMES, a0));
                    L(o, pd + "world.setWorldTime(" + TIME_VAL[ti] + "L);");
                    break;
                }
                case B_WEATHER: {
                    String w = choiceOr(a0, WEATHER, "limpo");
                    if (w.equals("limpo")) {
                        L(o, pd + "world.getWorldInfo().setRaining(false);");
                        L(o, pd + "world.getWorldInfo().setThundering(false);");
                    } else if (w.equals("chuva")) {
                        L(o, pd + "world.getWorldInfo().setRaining(true);");
                        L(o, pd + "world.getWorldInfo().setRainTime(6000);");
                    } else {
                        L(o, pd + "world.getWorldInfo().setRaining(true);");
                        L(o, pd + "world.getWorldInfo().setThundering(true);");
                        L(o, pd + "world.getWorldInfo().setRainTime(6000);");
                        L(o, pd + "world.getWorldInfo().setThunderTime(6000);");
                    }
                    break;
                }
                case B_FIRE:
                    L(o, pd + "player.setFire(" + I(a0, 5) + ");");
                    break;
                case B_LAUNCH:
                    L(o, pd + "player.motionY = " + D(a0, 1) + ";");
                    L(o, pd + "player.velocityChanged = true;");
                    break;
                case B_FLY:
                    if (a0.equals("desligado")) {
                        L(o, pd + "player.capabilities.allowFlying = false;");
                        L(o, pd + "player.capabilities.isFlying = false;");
                    } else {
                        L(o, pd + "player.capabilities.allowFlying = true;");
                    }
                    L(o, pd + "player.sendPlayerAbilities();");
                    break;
                case B_SPEED:
                    L(o, pd + "player.capabilities.setPlayerWalkSpeed((float) " + D(a0, 0.1) + ");");
                    L(o, pd + "player.sendPlayerAbilities();");
                    break;
                case B_CLEARINV:
                    L(o, pd + "player.inventory.clearInventory(null, -1);");
                    break;
                case B_KILL:
                    L(o, pd + "killAround(player, " + D(a0, 8) + ");");
                    break;
                case B_CANCEL:
                    L(o, pd + "if (ev != null && ev.isCancelable()) ev.setCanceled(true);");
                    break;
                case B_CALL: {
                    int idx = -1;
                    for (int k = 0; k < p.scripts.size(); k++) if (p.scripts.get(k).name.equals(a0)) idx = k;
                    if (idx < 0) warns.add("Script " + s.name + ": chamada para o script \"" + a0 + "\" que nao existe (ignorada).");
                    else L(o, pd + "callScript(" + idx + ", player, world, ev);");
                    break;
                }
                case B_WAIT:
                    L(o, pd + "schedule((int) (" + D(a0, 3) + " * 20.0D), new Runnable() { public void run() {");
                    st.add('w');
                    break;
                case B_IF_HEALTH:
                    L(o, pd + "if (player.getHealth() / 2.0F " + choiceOr(a0, HOPS, "<") + " " + D(a1, 5) + ") {");
                    st.add('i');
                    break;
                case B_IF_SNEAK:
                    L(o, pd + "if (player.isSneaking()) {");
                    st.add('i');
                    break;
                case B_IF_TIME:
                    L(o, pd + "if (" + (a0.equals("dia") ? "" : "!") + "world.isDaytime()) {");
                    st.add('i');
                    break;
                case B_IF_RAIN:
                    L(o, pd + "if (world.isRaining()) {");
                    st.add('i');
                    break;
                case B_IF_ITEM:
                    L(o, pd + "if (hasItem(player, \"" + esc(a0) + "\")) {");
                    st.add('i');
                    break;
                case B_IF_VAR:
                    L(o, pd + "if (" + v + " " + choiceOr(a1, OPS, ">") + " " + D(a2, 10) + ") {");
                    st.add('i');
                    break;
                case B_IF_HEIGHT:
                    L(o, pd + "if (player.posY " + choiceOr(a0, HOPS, ">") + " " + D(a1, 100) + ") {");
                    st.add('i');
                    break;
                case B_IF_CHANCE:
                    L(o, pd + "if (RND.nextInt(100) < " + I(a0, 50) + ") {");
                    st.add('i');
                    break;
                case B_REPEAT:
                    L(o, pd + "for (int i" + loops + " = 0; i" + loops + " < " + I(a0, 3) + "; i" + loops + "++) {");
                    loops++;
                    st.add('r');
                    break;
                case B_ELSE: {
                    while (!st.isEmpty() && st.get(st.size() - 1) == 'w') {
                        st.remove(st.size() - 1);
                        L(o, pad(2 + st.size()) + "} });");
                    }
                    if (!st.isEmpty() && st.get(st.size() - 1) == 'i') {
                        st.set(st.size() - 1, 'e');
                        L(o, pad(2 + st.size() - 1) + "} else {");
                    } else {
                        warns.add("Script " + s.name + ": \"Senao\" sem \"Se\" (ignorado).");
                    }
                    break;
                }
                case B_END: {
                    while (!st.isEmpty() && st.get(st.size() - 1) == 'w') {
                        st.remove(st.size() - 1);
                        L(o, pad(2 + st.size()) + "} });");
                    }
                    if (!st.isEmpty()) {
                        st.remove(st.size() - 1);
                        L(o, pad(2 + st.size()) + "}");
                    } else {
                        warns.add("Script " + s.name + ": \"Fim\" sem bloco aberto (ignorado).");
                    }
                    break;
                }
                case B_VSET:
                    L(o, pd + v + " = " + D(a1, 0) + ";");
                    break;
                case B_VADD:
                    L(o, pd + v + " += " + D(a1, 1) + ";");
                    break;
                case B_VSUB:
                    L(o, pd + v + " -= " + D(a1, 1) + ";");
                    break;
                case B_VMUL:
                    L(o, pd + v + " *= " + D(a1, 2) + ";");
                    break;
                case B_VRAND:
                    L(o, pd + v + " = " + D(a1, 1) + " + RND.nextInt(Math.max(1, (int) (" + D(a2, 10) + " - " + D(a1, 1) + ") + 1));");
                    break;
                case B_VSHOW:
                    L(o, pd + "msg(player, \"" + esc(lowerId(a0, "v")) + " = \" + " + v + ");");
                    break;
                default:
                    break;
            }
        }
        while (!st.isEmpty()) {
            char t = st.remove(st.size() - 1);
            L(o, pad(2 + st.size()) + (t == 'w' ? "} });" : "}"));
            warns.add("Script " + s.name + ": um bloco ficou sem \"Fim\" e foi fechado no final.");
        }
    }

    // ============================================================================================
    // EDITOR 2D (TELA)
    // ============================================================================================
    @SideOnly(Side.CLIENT)
    public static class GuiMaker extends GuiScreen {
        static final String[] TABS = {"Projeto", "Itens", "Blocos", "Scripts", "Vars", "Codigo"};

        interface Setter {
            void set(String s);
        }

        static class Btn {
            int x, y, w, h;
            String id;
            int d;
        }

        static class Fld {
            GuiTextField tf;
            Setter set;
            String label;
            int lx, ly;
        }

        int tab = 0, selItem = 0, selBlock = 0, selScript = 0, selCmd = -1, selVar = 0, selProj = -1;
        int scrollL = 0, scrollR = 0, palScroll = 0, palCat = 0, codeScroll = 0;
        boolean palette = false;
        String info = "Toque nas abas para montar o seu mod.";
        String[] code = new String[0];
        String lastCode = "";
        final ArrayList<String> warns = new ArrayList<String>();
        final ArrayList<Btn> btns = new ArrayList<Btn>();
        final ArrayList<Fld> flds = new ArrayList<Fld>();
        final ArrayList<Integer> palMap = new ArrayList<Integer>();
        int mx0 = 0, my0 = 0;

        int LX() {
            return 6;
        }

        int LW() {
            return Math.min(112, width / 3);
        }

        int RX() {
            return LX() + LW() + 8;
        }

        int RW() {
            return width - RX() - 6;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return true;
        }

        @Override
        public void initGui() {
            Keyboard.enableRepeatEvents(true);
            buildFields();
        }

        @Override
        public void onGuiClosed() {
            Keyboard.enableRepeatEvents(false);
            try {
                saveProject(P);
            } catch (Exception ex) {
                // ignora
            }
        }

        @Override
        public void updateScreen() {
            for (Fld f : flds) f.tf.updateCursorCounter();
        }

        // ---------- desenho ----------
        void txt(String s, int x, int y, int c) {
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(s, Math.max(10, width - x - 4)), x, y, c);
        }

        void addBtn(String id, int d, int x, int y, int w, int h) {
            Btn b = new Btn();
            b.x = x;
            b.y = y;
            b.w = w;
            b.h = h;
            b.id = id;
            b.d = d;
            btns.add(b);
        }

        void button(String id, int d, int x, int y, int w, int h, String label, boolean active) {
            boolean hover = mx0 >= x && mx0 <= x + w && my0 >= y && my0 <= y + h;
            drawRect(x, y, x + w, y + h, active ? 0xFF2F6FD0 : (hover ? 0xFF3A4A66 : 0xFF263248));
            drawRect(x, y, x + w, y + 1, 0xFF4A5B7A);
            String t = fontRendererObj.trimStringToWidth(label, Math.max(4, w - 4));
            fontRendererObj.drawString(t, x + (w - fontRendererObj.getStringWidth(t)) / 2, y + (h - 8) / 2, 0xFFFFFFFF);
            addBtn(id, d, x, y, w, h);
        }

        void rows(String id, int x, int y, int w, int h, List<String> items, int sel, int scroll) {
            drawRect(x, y, x + w, y + h, 0xFF0B121C);
            int vis = h / 12;
            for (int i = 0; i < vis; i++) {
                int idx = i + scroll;
                if (idx >= items.size()) break;
                int ry = y + i * 12;
                if (idx == sel) drawRect(x, ry, x + w, ry + 12, 0xFF2F6FD0);
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(items.get(idx), Math.max(4, w - 6)), x + 3, ry + 2,
                    idx == sel ? 0xFFFFFFFF : 0xFFC8D4E8);
                addBtn(id, idx, x, ry, w, 12);
            }
        }

        void fld(String label, int x, int y, int w, String text, Setter set) {
            Fld f = new Fld();
            f.tf = new GuiTextField(fontRendererObj, x, y, Math.max(20, w), 14);
            f.tf.setMaxStringLength(160);
            f.tf.setText(text);
            f.set = set;
            f.label = label;
            f.lx = x;
            f.ly = y - 9;
            flds.add(f);
        }

        Script curScript() {
            return selScript >= 0 && selScript < P.scripts.size() ? P.scripts.get(selScript) : null;
        }

        Cmd curCmd() {
            Script s = curScript();
            return s != null && selCmd >= 0 && selCmd < s.cmds.size() ? s.cmds.get(selCmd) : null;
        }

        @Override
        public void drawScreen(int mx, int my, float pt) {
            mx0 = mx;
            my0 = my;
            btns.clear();
            drawRect(0, 0, width, height, 0xFF101822);
            int tw = width / TABS.length;
            for (int i = 0; i < TABS.length; i++) button("tab", i, i * tw, 0, tw - 1, 16, TABS[i], tab == i);
            switch (tab) {
                case 0:
                    drawProject();
                    break;
                case 1:
                    drawItems();
                    break;
                case 2:
                    drawBlocks();
                    break;
                case 3:
                    drawScripts();
                    break;
                case 4:
                    drawVars();
                    break;
                default:
                    drawCode();
                    break;
            }
            for (Fld f : flds) {
                if (f.label.length() > 0) fontRendererObj.drawString(f.label, f.lx, f.ly, 0xFF9FB0CC);
                f.tf.drawTextBox();
            }
            if (palette) drawPalette();
            txt(info, 4, height - 10, 0xFFFFD27F);
        }

        void drawProject() {
            int half = width / 2;
            int w4 = (width - 30) / 4;
            button("proj_save", 0, 6, 150, w4, 14, "Salvar", false);
            button("proj_load", 0, 10 + w4, 150, w4, 14, "Recarregar", false);
            button("proj_new", 0, 14 + 2 * w4, 150, w4, 14, "Novo vazio", false);
            button("proj_sample", 0, 18 + 3 * w4, 150, w4, 14, "Exemplo", false);
            txt("Projetos salvos (toque para abrir):", half + 6, 24, 0xFFFFFFFF);
            ArrayList<String> names = listProjects();
            rows("proj_sel", half + 6, 36, width - half - 12, 100, names, selProj, 0);
            txt("1) Monte itens, blocos e scripts nas abas.", 6, height - 50, 0xFFB8C4DA);
            txt("2) Aba Codigo: Gerar > Salvar .java ou Copiar.", 6, height - 40, 0xFFB8C4DA);
            txt("3) Coloque o .java no repositorio e compile no GitHub.", 6, height - 30, 0xFFB8C4DA);
            txt("Dica: cada projeto precisa de um modId diferente.", 6, height - 20, 0xFFB8C4DA);
        }

        void drawItems() {
            int lx = LX(), lw = LW(), rx = RX(), rw = RW();
            ArrayList<String> names = new ArrayList<String>();
            for (ItemDef it : P.items) names.add(it.display + " (" + it.id + ")");
            int listH = height - 24 - 36;
            rows("item_sel", lx, 24, lw, listH, names, selItem, scrollL);
            int q = lw / 4;
            int by = 24 + listH + 2;
            button("item_add", 0, lx, by, q - 1, 14, "+", false);
            button("item_del", 0, lx + q, by, q - 1, 14, "-", false);
            button("scrup", 0, lx + 2 * q, by, q - 1, 14, "^", false);
            button("scrdn", 0, lx + 3 * q, by, q - 1, 14, "v", false);
            if (selItem < 0 || selItem >= P.items.size()) {
                txt("Toque em + para criar um item.", rx, 40, 0xFFB8C4DA);
                return;
            }
            ItemDef it = P.items.get(selItem);
            button("item_icon", 0, rx, 106, rw, 14, "Icone: " + ITEM_ICONS[it.icon], false);
            button("item_stack", 0, rx, 124, rw, 14, "Tamanho da pilha: " + it.stack, false);
            txt("Receita sem formato: 1 a 3 ingredientes (minecraft:stick).", rx, 146, 0xFFB8C4DA);
            txt("Script: aba Scripts > evento \"Ao clicar com item do mod\".", rx, 158, 0xFFB8C4DA);
        }

        void drawBlocks() {
            int lx = LX(), lw = LW(), rx = RX(), rw = RW();
            ArrayList<String> names = new ArrayList<String>();
            for (BlockDef b : P.blocks) names.add(b.display + " (" + b.id + ")");
            int listH = height - 24 - 36;
            rows("blk_sel", lx, 24, lw, listH, names, selBlock, scrollL);
            int q = lw / 4;
            int by = 24 + listH + 2;
            button("blk_add", 0, lx, by, q - 1, 14, "+", false);
            button("blk_del", 0, lx + q, by, q - 1, 14, "-", false);
            button("scrup", 0, lx + 2 * q, by, q - 1, 14, "^", false);
            button("scrdn", 0, lx + 3 * q, by, q - 1, 14, "v", false);
            if (selBlock < 0 || selBlock >= P.blocks.size()) {
                txt("Toque em + para criar um bloco.", rx, 40, 0xFFB8C4DA);
                return;
            }
            BlockDef b = P.blocks.get(selBlock);
            button("blk_icon", 0, rx, 130, rw, 14, "Aparencia: " + BLOCK_ICONS[b.base], false);
            txt("Dureza e brilho (0 a 15). Receita: 1 a 3 ingredientes.", rx, 152, 0xFFB8C4DA);
            txt("Script: evento \"Ao clicar no bloco do mod\".", rx, 164, 0xFFB8C4DA);
        }

        void drawVars() {
            int lx = LX(), lw = LW(), rx = RX();
            ArrayList<String> names = new ArrayList<String>();
            for (VarDef v : P.vars) names.add(v.name + " = " + v.init);
            int listH = height - 24 - 36;
            rows("var_sel", lx, 24, lw, listH, names, selVar, scrollL);
            int q = lw / 4;
            int by = 24 + listH + 2;
            button("var_add", 0, lx, by, q - 1, 14, "+", false);
            button("var_del", 0, lx + q, by, q - 1, 14, "-", false);
            button("scrup", 0, lx + 2 * q, by, q - 1, 14, "^", false);
            button("scrdn", 0, lx + 3 * q, by, q - 1, 14, "v", false);
            txt("Variaveis sao numeros guardados no mod (ex.: pontos).", rx, 100, 0xFFB8C4DA);
            txt("Use os blocos de Variaveis e de Controle nos scripts.", rx, 112, 0xFFB8C4DA);
            txt("Valem para todos os jogadores e zeram ao reiniciar.", rx, 124, 0xFFB8C4DA);
        }

        void drawScripts() {
            int lx = LX(), lw = LW(), rx = RX(), rw = RW();
            ArrayList<String> names = new ArrayList<String>();
            for (Script s : P.scripts) names.add(s.name);
            rows("scr_sel", lx, 24, lw, 96, names, selScript, scrollL);
            int q = lw / 4;
            button("scr_add", 0, lx, 122, q - 1, 14, "+", false);
            button("scr_del", 0, lx + q, 122, q - 1, 14, "-", false);
            button("scrup", 0, lx + 2 * q, 122, q - 1, 14, "^", false);
            button("scrdn", 0, lx + 3 * q, 122, q - 1, 14, "v", false);
            Script s = curScript();
            if (s == null) {
                txt("Toque em + para criar um script.", rx, 40, 0xFFB8C4DA);
                return;
            }
            int evw = Math.min(150, rw);
            txt("Evento (toque para trocar)", rx, 27, 0xFF9FB0CC);
            button("scr_event", 0, rx, 36, evw, 14, EV_NAME[s.event], false);
            if (s.event == EV_ITEM || s.event == EV_BLOCK) {
                button("scr_arg", 0, rx + evw + 4, 36, rw - evw - 4, 14, s.arg.length() == 0 ? "(escolha)" : s.arg, false);
            }
            int listH = Math.max(24, height - 58 - 54 - 4);
            int[] ind = indents(s.cmds);
            ArrayList<String> rowsTxt = new ArrayList<String>();
            for (int i = 0; i < s.cmds.size(); i++) {
                StringBuilder sp2 = new StringBuilder();
                for (int k = 0; k < ind[i]; k++) sp2.append("  ");
                rowsTxt.add((i + 1) + ". " + sp2 + labelText(s.cmds.get(i)));
            }
            rows("cmd_sel", rx, 54, rw, listH, rowsTxt, selCmd, scrollR);
            Cmd c = curCmd();
            if (c != null) {
                txt(labelText(c), rx, height - 58, 0xFFFFFFFF);
                String[] spec = BSPEC[c.type];
                int n = spec.length;
                int py = height - 46;
                for (int i = 0; i < n; i++) {
                    if (isTextSpec(spec[i])) continue;
                    int w = (rw - (n - 1) * 4) / n;
                    String val = c.a[i].length() == 0 ? "(nenhum)" : c.a[i];
                    button("pcyc", i, rx + i * (w + 4), py, w, 14, val, false);
                }
            }
            int bw = (rw - 6 * 3) / 7;
            int bx = rx;
            int byy = height - 28;
            String[] lb = {"+Bloco", "Sobe", "Desce", "Copia", "Apaga", "^", "v"};
            String[] ids = {"cmd_add", "cmd_up", "cmd_down", "cmd_dup", "cmd_del", "scrup", "scrdn"};
            int[] ds = {0, 0, 0, 0, 0, 1, 1};
            for (int i = 0; i < 7; i++) button(ids[i], ds[i], bx + i * (bw + 3), byy, bw, 14, lb[i], false);
        }

        void drawPalette() {
            int rx = RX(), rw = RW(), top = 36, h = height - top - 14;
            drawRect(rx - 2, top - 2, rx + rw + 2, top + h, 0xFF16233A);
            addBtn("noop", 0, rx - 2, top - 2, rw + 4, h + 2);
            txt("Escolha um bloco para inserir:", rx + 2, top + 1, 0xFFFFFFFF);
            int cw = (rw - 8) / 3;
            for (int i = 0; i < 3; i++) button("pal_cat", i, rx + i * (cw + 4), top + 12, cw, 14, CATS[i], palCat == i);
            ArrayList<String> names = new ArrayList<String>();
            palMap.clear();
            for (int t = 0; t < NB; t++) {
                if (BCAT[t] != palCat) continue;
                names.add(BLABEL[t].replaceAll("\\{[0-9]\\}", "_"));
                palMap.add(Integer.valueOf(t));
            }
            int listH = h - 28 - 20;
            rows("pal_pick", rx, top + 28, rw, listH, names, -1, palScroll);
            int by = top + h - 16;
            button("pal_close", 0, rx, by, rw - 52, 14, "Fechar", false);
            button("scrup", 2, rx + rw - 50, by, 24, 14, "^", false);
            button("scrdn", 2, rx + rw - 24, by, 24, 14, "v", false);
        }

        void drawCode() {
            int w4 = (width - 30) / 4;
            button("gen", 0, 6, 22, w4, 14, "Gerar", false);
            button("savejava", 0, 10 + w4, 22, w4, 14, "Salvar .java", false);
            button("copy", 0, 14 + 2 * w4, 22, w4, 14, "Copiar", false);
            button("scrup", 3, 18 + 3 * w4, 22, w4 / 2 - 1, 14, "^", false);
            button("scrdn", 3, 18 + 3 * w4 + w4 / 2, 22, w4 / 2 - 1, 14, "v", false);
            int y = 42;
            for (int i = 0; i < warns.size() && i < 2; i++) {
                txt("Aviso: " + warns.get(i), 6, y, 0xFFFFA64D);
                y += 10;
            }
            int lines = Math.max(1, (height - y - 14) / 9);
            for (int i = 0; i < lines; i++) {
                int idx = i + codeScroll;
                if (idx >= code.length) break;
                String ln = String.format("%3d ", idx + 1) + code[idx].replace("\t", "    ");
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ln, width - 10), 6, y + i * 9, 0xFFB5E3B5);
            }
            if (code.length == 0) txt("Toque em Gerar para ver o codigo do mod.", 6, y, 0xFFB8C4DA);
        }

        // ---------- campos de texto ----------
        void buildFields() {
            flds.clear();
            int lx = LX(), lw = LW(), rx = RX(), rw = RW();
            switch (tab) {
                case 0: {
                    int fw = width / 2 - 12;
                    fld("modId (so letras minusculas)", 6, 34, fw, P.modId, new Setter() {
                        public void set(String v) {
                            P.modId = v;
                        }
                    });
                    fld("Nome do mod", 6, 58, fw, P.name, new Setter() {
                        public void set(String v) {
                            P.name = v;
                        }
                    });
                    fld("Versao", 6, 82, fw, P.ver, new Setter() {
                        public void set(String v) {
                            P.ver = v;
                        }
                    });
                    fld("Pacote (igual ao do seu repositorio)", 6, 106, fw, P.pkg, new Setter() {
                        public void set(String v) {
                            P.pkg = v;
                        }
                    });
                    fld("Nome da classe (e do arquivo .java)", 6, 130, fw, P.cls, new Setter() {
                        public void set(String v) {
                            P.cls = v;
                        }
                    });
                    break;
                }
                case 1:
                    if (selItem >= 0 && selItem < P.items.size()) {
                        final ItemDef it = P.items.get(selItem);
                        fld("ID do item (sem espacos)", rx, 34, rw, it.id, new Setter() {
                            public void set(String v) {
                                it.id = v;
                            }
                        });
                        fld("Nome exibido", rx, 58, rw, it.display, new Setter() {
                            public void set(String v) {
                                it.display = v;
                            }
                        });
                        int w3 = (rw - 8) / 3;
                        fld("Ingrediente 1", rx, 90, w3, it.r1, new Setter() {
                            public void set(String v) {
                                it.r1 = v;
                            }
                        });
                        fld("Ingrediente 2", rx + w3 + 4, 90, w3, it.r2, new Setter() {
                            public void set(String v) {
                                it.r2 = v;
                            }
                        });
                        fld("Ingrediente 3", rx + 2 * (w3 + 4), 90, w3, it.r3, new Setter() {
                            public void set(String v) {
                                it.r3 = v;
                            }
                        });
                    }
                    break;
                case 2:
                    if (selBlock >= 0 && selBlock < P.blocks.size()) {
                        final BlockDef b = P.blocks.get(selBlock);
                        fld("ID do bloco (sem espacos)", rx, 34, rw, b.id, new Setter() {
                            public void set(String v) {
                                b.id = v;
                            }
                        });
                        fld("Nome exibido", rx, 58, rw, b.display, new Setter() {
                            public void set(String v) {
                                b.display = v;
                            }
                        });
                        int w2 = (rw - 4) / 2;
                        fld("Dureza", rx, 82, w2, b.hard, new Setter() {
                            public void set(String v) {
                                b.hard = v;
                            }
                        });
                        fld("Brilho (0-15)", rx + w2 + 4, 82, w2, b.light, new Setter() {
                            public void set(String v) {
                                b.light = v;
                            }
                        });
                        int w3 = (rw - 8) / 3;
                        fld("Ingrediente 1", rx, 106, w3, b.r1, new Setter() {
                            public void set(String v) {
                                b.r1 = v;
                            }
                        });
                        fld("Ingrediente 2", rx + w3 + 4, 106, w3, b.r2, new Setter() {
                            public void set(String v) {
                                b.r2 = v;
                            }
                        });
                        fld("Ingrediente 3", rx + 2 * (w3 + 4), 106, w3, b.r3, new Setter() {
                            public void set(String v) {
                                b.r3 = v;
                            }
                        });
                    }
                    break;
                case 3: {
                    final Script s = curScript();
                    if (s != null) {
                        fld("Nome do script", lx, 152, lw, s.name, new Setter() {
                            public void set(String v) {
                                s.name = v;
                            }
                        });
                        int evw = Math.min(150, rw);
                        if (s.event == EV_TIME || s.event == EV_CMD) {
                            fld(s.event == EV_TIME ? "Segundos" : "Comando (sem /)", rx + evw + 4, 36, rw - evw - 4, s.arg, new Setter() {
                                public void set(String v) {
                                    s.arg = v;
                                }
                            });
                        }
                        final Cmd c = curCmd();
                        if (c != null) {
                            String[] spec = BSPEC[c.type];
                            int n = spec.length;
                            int py = height - 46;
                            for (int i = 0; i < n; i++) {
                                if (!isTextSpec(spec[i])) continue;
                                final int pi = i;
                                int w = (rw - (n - 1) * 4) / n;
                                fld("", rx + i * (w + 4), py, w, c.a[i], new Setter() {
                                    public void set(String v) {
                                        c.a[pi] = v;
                                    }
                                });
                            }
                        }
                    }
                    break;
                }
                case 4:
                    if (selVar >= 0 && selVar < P.vars.size()) {
                        final VarDef v0 = P.vars.get(selVar);
                        fld("Nome da variavel", rx, 34, rw, v0.name, new Setter() {
                            public void set(String v) {
                                v0.name = v;
                            }
                        });
                        fld("Valor inicial", rx, 58, rw, v0.init, new Setter() {
                            public void set(String v) {
                                v0.init = v;
                            }
                        });
                    }
                    break;
                default:
                    break;
            }
        }

        // ---------- entrada ----------
        @Override
        protected void mouseClicked(int mx, int my, int b) {
            if (!palette) {
                for (Fld f : flds) f.tf.mouseClicked(mx, my, b);
            }
            for (int i = btns.size() - 1; i >= 0; i--) {
                Btn bt = btns.get(i);
                if (mx >= bt.x && mx <= bt.x + bt.w && my >= bt.y && my <= bt.y + bt.h) {
                    act(bt.id, bt.d);
                    return;
                }
            }
        }

        @Override
        protected void keyTyped(char c, int key) {
            if (!palette) {
                for (Fld f : flds) {
                    if (f.tf.isFocused()) {
                        if (key == Keyboard.KEY_ESCAPE) {
                            f.tf.setFocused(false);
                            return;
                        }
                        f.tf.textboxKeyTyped(c, key);
                        f.set.set(f.tf.getText());
                        return;
                    }
                }
            }
            if (key == Keyboard.KEY_ESCAPE) {
                if (palette) palette = false;
                else mc.displayGuiScreen(null);
            }
        }

        void clampScroll(int which, int size, int delta) {
            int step = which == 3 ? 5 : 1;
            int v;
            if (which == 1) v = scrollR;
            else if (which == 2) v = palScroll;
            else if (which == 3) v = codeScroll;
            else v = scrollL;
            v = Math.max(0, Math.min(Math.max(0, size - 1), v + delta * step));
            if (which == 1) scrollR = v;
            else if (which == 2) palScroll = v;
            else if (which == 3) codeScroll = v;
            else scrollL = v;
        }

        int listSize(int which) {
            if (which == 1) {
                Script s = curScript();
                return s == null ? 0 : s.cmds.size();
            }
            if (which == 2) return palMap.size();
            if (which == 3) return code.length;
            if (tab == 1) return P.items.size();
            if (tab == 2) return P.blocks.size();
            if (tab == 3) return P.scripts.size();
            if (tab == 4) return P.vars.size();
            return 0;
        }

        String nextId(String base, int n) {
            return base + (n + 1);
        }

        void act(String id, int d) {
            if (id.equals("noop")) return;
            if (id.equals("tab")) {
                tab = d;
                palette = false;
                selCmd = -1;
                scrollL = 0;
                scrollR = 0;
                buildFields();
                return;
            }
            if (id.equals("scrup")) {
                clampScroll(d, listSize(d), -1);
                return;
            }
            if (id.equals("scrdn")) {
                clampScroll(d, listSize(d), 1);
                return;
            }
            // projeto
            if (id.equals("proj_save")) {
                try {
                    saveProject(P);
                    info = "Projeto salvo: " + new File(pdir(), projFileName(P)).getAbsolutePath();
                } catch (Exception ex) {
                    info = "Erro ao salvar: " + ex;
                }
            } else if (id.equals("proj_load") || id.equals("proj_sel")) {
                try {
                    String name = id.equals("proj_sel") ? listProjects().get(d) : lowerId(P.modId, "meumod");
                    Proj lp = loadProject(name);
                    if (lp != null) {
                        P = lp;
                        selItem = 0;
                        selBlock = 0;
                        selScript = 0;
                        selCmd = -1;
                        selVar = 0;
                        selProj = id.equals("proj_sel") ? d : selProj;
                        code = new String[0];
                        info = "Projeto aberto: " + name;
                        buildFields();
                    } else {
                        info = "Esse projeto ainda nao foi salvo.";
                    }
                } catch (Exception ex) {
                    info = "Erro ao abrir: " + ex;
                }
            } else if (id.equals("proj_new") || id.equals("proj_sample")) {
                try {
                    saveProject(P);
                } catch (Exception ex) {
                    // ignora
                }
                ArrayList<String> ex = listProjects();
                String nid = "meumod";
                int n = 2;
                while (ex.contains(nid)) nid = "meumod" + (n++);
                P = id.equals("proj_new") ? emptyProject(nid) : sample();
                P.modId = nid;
                selItem = 0;
                selBlock = 0;
                selScript = 0;
                selCmd = -1;
                selVar = 0;
                code = new String[0];
                info = "Novo projeto criado (" + nid + "). Troque o modId e o nome.";
                buildFields();
            } else if (id.equals("item_sel")) {
                selItem = d;
                buildFields();
            } else if (id.equals("item_add")) {
                ItemDef it = new ItemDef();
                it.id = nextId("item", P.items.size());
                it.display = "Item " + (P.items.size() + 1);
                P.items.add(it);
                selItem = P.items.size() - 1;
                buildFields();
            } else if (id.equals("item_del")) {
                if (selItem >= 0 && selItem < P.items.size()) P.items.remove(selItem);
                selItem = Math.max(0, Math.min(selItem, P.items.size() - 1));
                buildFields();
            } else if (id.equals("item_icon")) {
                if (selItem >= 0 && selItem < P.items.size()) {
                    ItemDef it = P.items.get(selItem);
                    it.icon = (it.icon + 1) % ITEM_ICONS.length;
                }
            } else if (id.equals("item_stack")) {
                if (selItem >= 0 && selItem < P.items.size()) {
                    ItemDef it = P.items.get(selItem);
                    it.stack = it.stack == 64 ? 16 : (it.stack == 16 ? 1 : 64);
                }
            } else if (id.equals("blk_sel")) {
                selBlock = d;
                buildFields();
            } else if (id.equals("blk_add")) {
                BlockDef b = new BlockDef();
                b.id = nextId("bloco", P.blocks.size());
                b.display = "Bloco " + (P.blocks.size() + 1);
                P.blocks.add(b);
                selBlock = P.blocks.size() - 1;
                buildFields();
            } else if (id.equals("blk_del")) {
                if (selBlock >= 0 && selBlock < P.blocks.size()) P.blocks.remove(selBlock);
                selBlock = Math.max(0, Math.min(selBlock, P.blocks.size() - 1));
                buildFields();
            } else if (id.equals("blk_icon")) {
                if (selBlock >= 0 && selBlock < P.blocks.size()) {
                    BlockDef b = P.blocks.get(selBlock);
                    b.base = (b.base + 1) % BLOCK_ICONS.length;
                }
            } else if (id.equals("var_sel")) {
                selVar = d;
                buildFields();
            } else if (id.equals("var_add")) {
                VarDef v = new VarDef();
                v.name = "var" + (P.vars.size() + 1);
                P.vars.add(v);
                selVar = P.vars.size() - 1;
                buildFields();
            } else if (id.equals("var_del")) {
                if (selVar >= 0 && selVar < P.vars.size()) P.vars.remove(selVar);
                selVar = Math.max(0, Math.min(selVar, P.vars.size() - 1));
                buildFields();
            } else if (id.equals("scr_sel")) {
                selScript = d;
                selCmd = -1;
                scrollR = 0;
                buildFields();
            } else if (id.equals("scr_add")) {
                Script s = new Script();
                s.name = "script" + (P.scripts.size() + 1);
                P.scripts.add(s);
                selScript = P.scripts.size() - 1;
                selCmd = -1;
                buildFields();
            } else if (id.equals("scr_del")) {
                if (selScript >= 0 && selScript < P.scripts.size()) P.scripts.remove(selScript);
                selScript = Math.max(0, Math.min(selScript, P.scripts.size() - 1));
                selCmd = -1;
                buildFields();
            } else if (id.equals("scr_event")) {
                Script s = curScript();
                if (s != null) {
                    s.event = (s.event + 1) % EV_NAME.length;
                    if (s.event == EV_TIME) s.arg = "10";
                    else if (s.event == EV_CMD) s.arg = "comando";
                    else if (s.event == EV_ITEM) s.arg = P.items.size() > 0 ? P.items.get(0).id : "";
                    else if (s.event == EV_BLOCK) s.arg = P.blocks.size() > 0 ? P.blocks.get(0).id : "";
                    buildFields();
                }
            } else if (id.equals("scr_arg")) {
                Script s = curScript();
                if (s != null) {
                    ArrayList<String> ids = new ArrayList<String>();
                    if (s.event == EV_ITEM) for (ItemDef it : P.items) ids.add(it.id);
                    else for (BlockDef b : P.blocks) ids.add(b.id);
                    if (ids.isEmpty()) {
                        info = "Crie um item ou bloco primeiro.";
                    } else {
                        int i = ids.indexOf(s.arg);
                        s.arg = ids.get((i + 1) % ids.size());
                    }
                }
            } else if (id.equals("cmd_sel")) {
                selCmd = d;
                buildFields();
            } else if (id.equals("cmd_add")) {
                if (curScript() == null) info = "Crie um script primeiro.";
                else {
                    palette = true;
                    palScroll = 0;
                }
            } else if (id.equals("pal_cat")) {
                palCat = d;
                palScroll = 0;
            } else if (id.equals("pal_close")) {
                palette = false;
            } else if (id.equals("pal_pick")) {
                Script s = curScript();
                if (s != null && d >= 0 && d < palMap.size()) {
                    Cmd c = mk(palMap.get(d).intValue());
                    fixDefaults(P, c);
                    int pos = selCmd >= 0 ? selCmd + 1 : s.cmds.size();
                    s.cmds.add(Math.min(pos, s.cmds.size()), c);
                    selCmd = Math.min(pos, s.cmds.size() - 1);
                    palette = false;
                    buildFields();
                }
            } else if (id.equals("cmd_up") || id.equals("cmd_down")) {
                Script s = curScript();
                if (s != null && selCmd >= 0 && selCmd < s.cmds.size()) {
                    int to = id.equals("cmd_up") ? selCmd - 1 : selCmd + 1;
                    if (to >= 0 && to < s.cmds.size()) {
                        Cmd c = s.cmds.remove(selCmd);
                        s.cmds.add(to, c);
                        selCmd = to;
                    }
                }
            } else if (id.equals("cmd_dup")) {
                Script s = curScript();
                Cmd c = curCmd();
                if (s != null && c != null) {
                    Cmd n = new Cmd();
                    n.type = c.type;
                    for (int i = 0; i < 4; i++) n.a[i] = c.a[i];
                    s.cmds.add(selCmd + 1, n);
                    selCmd++;
                    buildFields();
                }
            } else if (id.equals("cmd_del")) {
                Script s = curScript();
                if (s != null && selCmd >= 0 && selCmd < s.cmds.size()) {
                    s.cmds.remove(selCmd);
                    selCmd = Math.min(selCmd, s.cmds.size() - 1);
                    buildFields();
                }
            } else if (id.equals("pcyc")) {
                Cmd c = curCmd();
                if (c != null && d >= 0 && d < BSPEC[c.type].length) {
                    String[] lst = choicesOf(BSPEC[c.type][d]);
                    if (lst.length == 0) {
                        info = BSPEC[c.type][d].equals("v") ? "Crie uma variavel na aba Vars." : "Nada para escolher.";
                    } else {
                        int i = indexOf(lst, c.a[d]);
                        c.a[d] = lst[(i + 1) % lst.length];
                    }
                }
            } else if (id.equals("gen")) {
                lastCode = generate(P, warns);
                code = lastCode.split("\n", -1);
                codeScroll = 0;
                info = "Codigo gerado: " + code.length + " linhas, " + warns.size() + " aviso(s).";
            } else if (id.equals("savejava")) {
                try {
                    lastCode = generate(P, warns);
                    code = lastCode.split("\n", -1);
                    File f = new File(dir(), className(P.cls) + ".java");
                    BufferedWriter w = new B
