package com.xpgaming.xPHunt;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.Entity3HasStats;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.xpgaming.xPHunt.Commands.Hunt;
import com.xpgaming.xPHunt.Commands.NewHunt;
import com.xpgaming.xPHunt.Listeners.CaptureListener;
import com.xpgaming.xPHunt.Listeners.PixelmonSpawnListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin(id = Main.id, name = Main.name, version = "0.5",
        dependencies = {
                @Dependency(id = "pixelmon")
        })
public class Main {
    private static Main instance;
    public static Main getInstance() {
        return instance;
    }
    public static final String id = "xphunt";
    public static String pokemon1 = "", nature1 = "", nature1b = "", nature1c = "";
    public static String pokemon2 = "", nature2 = "", nature2b = "", nature2c = "";
    public static String pokemon3 = "", nature3 = "", nature3b = "", nature3c = "";
    public static String pokemon4 = "", nature4 = "", nature4b = "", nature4c = "";
    public static List<Entity> pixelmonList = new ArrayList<Entity>();
    public static LocalDateTime pokemon1expiry, pokemon2expiry, pokemon3expiry, pokemon4expiry;
    public static String pokemon1ballName, pokemon2ballName, pokemon3ballName, pokemon4ballName;
    public static ItemStack pokemon1ballReward, pokemon2ballReward, pokemon3ballReward, pokemon4ballReward;
    public static ItemStack pokemon1rc, pokemon2rc, pokemon3rc, pokemon4rc;
    public static BigDecimal pokemon1moneyReward, pokemon2moneyReward, pokemon3moneyReward, pokemon4moneyReward;
    public static Boolean initialised = false;
    public static final String name = "xP// Hunt";
    private static final Logger log = LoggerFactory.getLogger(name);

    private static EconomyService economyService;

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(EconomyService.class)) {
            economyService = (EconomyService) event.getNewProviderRegistration().getProvider();
        }
    }

    public void giveItemStack(ItemStack i, Player player) {
        if(player.getInventory().query(Hotbar.class, GridInventory.class).size() < 36)
        {
            player.getInventory().offer(i);
        }
        else
        {
            World world = player.getLocation().getExtent();
            Entity it = world
                    .createEntity(EntityTypes.ITEM, player.getLocation().getPosition());
            it.offer(Keys.REPRESENTED_ITEM, i.createSnapshot());
            SpawnCause spawnCause = SpawnCause.builder().type(SpawnTypes.PLUGIN).build();
            world.spawnEntity(it, Cause.source(spawnCause).build());
        }

    }

    public void addMoney(Player p, BigDecimal amount) {
        Optional<UniqueAccount> uOpt = economyService.getOrCreateAccount(p.getUniqueId());
        if(uOpt.isPresent()) {
            UniqueAccount account = uOpt.get();
            TransactionResult result = account.deposit(economyService.getDefaultCurrency(), amount, Cause.source(this).build());
            if (!(result.getResult() == ResultType.SUCCESS)) {
                p.sendMessage(Text.of("\u00A7f[\u00A7cHunt\u00A7f] \u00A7cUnable to give money, something broke!"));
            }
        }
    }

        CommandSpec hunt = CommandSpec.builder()
            .description(Text.of("List hunted pokemon!"))
            .permission("xpgaming.pokehunt.base.hunt")
            .executor(new Hunt())
            .build();

    CommandSpec newhunt = CommandSpec.builder()
            .description(Text.of("Renew hunted pokemon!"))
            .permission("xpgaming.pokehunt.admin.newhunt")
            .arguments(
                    GenericArguments.optional(GenericArguments.integer(Text.of("slot"))))
            .executor(new NewHunt())
            .build();

    @Listener
    public void onPixelmonMove(MoveEntityEvent event, @First Entity entity)
    {
        if(pixelmonList.contains(entity)) {
            EntityPixelmon pixelmon = (EntityPixelmon) entity;
            if (pixelmon.isEntityAlive() && !pixelmon.isInBall && !pixelmon.hasOwner()) {
                entity.getWorld().spawnParticles(ParticleEffect.builder().type(ParticleTypes.MOBSPAWNER_FLAMES).build(), entity.getLocation().getPosition(), 6);
            } else {
                pixelmonList.remove(entity);
                event.setCancelled(false);
            }
        }
    }

    @Listener (beforeModifications = true)
    public void onGameInitialization(GameInitializationEvent event) {
        instance = this;
        if(!initialised) {
            Utils.getInstance().initialisePokemon();
            initialised = true;
        }
        Sponge.getScheduler().createTaskBuilder().execute(() -> {
            if(Sponge.getServer().getWorld("Terra").isPresent()) {
                Sponge.getServer().getWorld("Terra").get().getEntities().stream().filter(ent -> ent instanceof Entity3HasStats).forEach(ent -> {
                    if (((Entity3HasStats) ent).getEntityData().hasKey("hasHuntParticles")) {
                        if (((Entity3HasStats) ent) != null && !((Entity3HasStats) ent).hasOwner()) {
                            ent.getWorld().spawnParticles(ParticleEffect.builder().type(ParticleTypes.MOBSPAWNER_FLAMES).build(), ent.getLocation().getPosition(), 3);
                        } else {
                            ((Entity3HasStats) ent).getEntityData().removeTag("hasHuntParticles");
                        }
                    }
                });
            }
        }).async().interval(1, TimeUnit.SECONDS).submit(this);

        Task task = Task.builder().execute(() -> {
            if(!Sponge.getServer().getOnlinePlayers().isEmpty()) {
                Text pokemon1 = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of("\u00A76"+Main.pokemon1))
                        .onHover(TextActions.showText(Text.of("\u00A76Nature 1: \u00A7e" + Main.nature1 + "\n\u00A76Nature 2: \u00A7e" + Main.nature1b + "\n\u00A76Nature 3: \u00A7e" + Main.nature1c)))
                        .build();
                Text pokemon1r = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of(" \u00A76[\u00A7eR\u00A76]"))
                        .onHover(TextActions.showText(Text.of("\u00A76Balls: \u00A7e" + Main.pokemon1ballReward.getQuantity() + " " + Main.pokemon1ballName + "\n\u00A76Money: \u00A7e" + Main.pokemon1moneyReward + " coins\n\u00A76Rare Candies: \u00A7e" + Main.pokemon1rc.getQuantity())))// + "\n\u00A76IVs: \u00A7e" + IVUpgrade(EnumNature.natureFromString(Main.nature1).index))))
                        .build();
                Text pokemon2 = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of("\u00A76"+Main.pokemon2))
                        .onHover(TextActions.showText(Text.of("\u00A76Nature 1: \u00A7e" + Main.nature2 + "\n\u00A76Nature 2: \u00A7e" + Main.nature2b + "\n\u00A76Nature 3: \u00A7e" + Main.nature2c)))
                        .build();
                Text pokemon2r = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of(" \u00A76[\u00A7eR\u00A76]"))
                        .onHover(TextActions.showText(Text.of("\u00A76Balls: \u00A7e" + Main.pokemon2ballReward.getQuantity() + " " + Main.pokemon2ballName + "\n\u00A76Money: \u00A7e" + Main.pokemon2moneyReward + " coins\n\u00A76Rare Candies: \u00A7e" + Main.pokemon2rc.getQuantity())))// + "\n\u00A76IVs: \u00A7e" + IVUpgrade(EnumNature.natureFromString(Main.nature1).index))))
                        .build();
                Text pokemon3 = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of("\u00A76"+Main.pokemon3))
                        .onHover(TextActions.showText(Text.of("\u00A76Nature 1: \u00A7e" + Main.nature3 + "\n\u00A76Nature 2: \u00A7e" + Main.nature3b + "\n\u00A76Nature 3: \u00A7e" + Main.nature3c)))
                        .build();
                Text pokemon3r = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of(" \u00A76[\u00A7eR\u00A76]"))
                        .onHover(TextActions.showText(Text.of("\u00A76Balls: \u00A7e" + Main.pokemon3ballReward.getQuantity() + " " + Main.pokemon3ballName + "\n\u00A76Money: \u00A7e" + Main.pokemon3moneyReward + " coins\n\u00A76Rare Candies: \u00A7e" + Main.pokemon3rc.getQuantity())))// + "\n\u00A76IVs: \u00A7e" + IVUpgrade(EnumNature.natureFromString(Main.nature1).index))))
                        .build();
                Text pokemon4 = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of("\u00A76"+Main.pokemon4))
                        .onHover(TextActions.showText(Text.of("\u00A76Nature 1: \u00A7e" + Main.nature4 + "\n\u00A76Nature 2: \u00A7e" + Main.nature4b + "\n\u00A76Nature 3: \u00A7e" + Main.nature4c)))
                        .build();
                Text pokemon4r = Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of(" \u00A76[\u00A7eR\u00A76]"))
                        .onHover(TextActions.showText(Text.of("\u00A76Balls: \u00A7e" + Main.pokemon4ballReward.getQuantity() + " " + Main.pokemon4ballName + "\n\u00A76Money: \u00A7e" + Main.pokemon4moneyReward + " coins\n\u00A76Rare Candies: \u00A7e" + Main.pokemon4rc.getQuantity())))// + "\n\u00A76IVs: \u00A7e" + IVUpgrade(EnumNature.natureFromString(Main.nature1).index))))
                        .build();
                Text comma = Text.of("\u00A7f, ");
                Text finalMessage2 = Text.builder()
                        .append(Text.of("\u00A7f[\u00A7aHunt\u00A7f] "))
                        .append(pokemon1)
                        .append(pokemon1r)
                        .append(comma)
                        .append(pokemon2)
                        .append(pokemon2r)
                        .append(comma)
                        .append(pokemon3)
                        .append(pokemon3r)
                        .append(comma)
                        .append(pokemon4)
                        .append(pokemon4r)
                        .build();
                Text finalMessage1 = Text.builder()
                        .append(Text.of("\u00A7f[\u00A7aHunt\u00A7f] Pok\u00E9mon currently in \u00A7a/hunt \u00A7fare: "))
                        .build();
                Sponge.getServer().getBroadcastChannel().send(finalMessage1);
                Sponge.getServer().getBroadcastChannel().send(finalMessage2);
            }
        })
                .interval(20, TimeUnit.MINUTES)
                .name("xP// Hunt Announcement")
                .submit(this);
        Sponge.getEventManager().registerListeners(this, new PixelmonSpawnListener());
        Pixelmon.EVENT_BUS.register(new CaptureListener());
        Sponge.getCommandManager().register(this, hunt, "hunt", "ph", "pokehunt", "hunts");
        Sponge.getCommandManager().register(this, newhunt, "newhunt", "nh");
        log.info("Loaded v0.5!");
    }
}
