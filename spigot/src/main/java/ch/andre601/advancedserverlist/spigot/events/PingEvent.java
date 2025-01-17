/*
 * MIT License
 *
 * Copyright (c) 2022 Andre_601
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package ch.andre601.advancedserverlist.spigot.events;

import ch.andre601.advancedserverlist.core.parsing.ComponentParser;
import ch.andre601.advancedserverlist.core.profiles.ProfileManager;
import ch.andre601.advancedserverlist.core.profiles.ServerListProfile;
import ch.andre601.advancedserverlist.core.profiles.replacer.StringReplacer;
import ch.andre601.advancedserverlist.core.profiles.replacer.placeholders.PlayerPlaceholders;
import ch.andre601.advancedserverlist.core.profiles.replacer.placeholders.ServerPlaceholders;
import ch.andre601.advancedserverlist.spigot.SpigotCore;
import ch.andre601.advancedserverlist.spigot.SpigotPlayer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;

import java.net.InetSocketAddress;
import java.util.*;

public class PingEvent implements Listener{
    
    private final SpigotCore plugin;
    private final ProtocolManager protocolManager;
    
    private final Map<String, String> hostAddresses = new HashMap<>();
    
    public PingEvent(SpigotCore plugin, ProtocolManager protocolManager){
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        
        loadPacketListener(plugin);
    }
    
    private void loadPacketListener(SpigotCore spigotPlugin){
        protocolManager.addPacketListener(new PacketAdapter(spigotPlugin, ListenerPriority.LOW, PacketType.Handshake.Client.SET_PROTOCOL){
            @Override
            public void onPacketReceiving(PacketEvent event){
                InetSocketAddress address = event.getPlayer().getAddress();
                if(address == null)
                    return;
                
                String host = event.getPacket().getStrings().read(0);
                
                hostAddresses.put(address.getHostString(), host);
            }
        });
        
        protocolManager.addPacketListener(new PacketAdapter(spigotPlugin, ListenerPriority.LOW, PacketType.Status.Server.SERVER_INFO){
            @Override
            public void onPacketSending(PacketEvent event){
                WrappedServerPing ping = event.getPacket().getServerPings().read(0);
                InetSocketAddress address = event.getPlayer().getAddress();
                if(address == null)
                    return;
    
                SpigotPlayer player = resolvePlayer(address, ping.getVersionProtocol());
                
                int online = ping.getPlayersOnline();
                int max = ping.getPlayersMaximum();
                
                PlayerPlaceholders playerPlaceholders = new PlayerPlaceholders(player);
                ServerPlaceholders serverPlaceholders = new ServerPlaceholders(online, max, hostAddresses.get(address.getHostString()));
                
                ServerListProfile profile = ProfileManager.get(spigotPlugin.getCore())
                    .replacements(playerPlaceholders)
                    .replacements(serverPlaceholders)
                    .getProfile();
                
                if(profile == null)
                    return;
    
                if(profile.isExtraPlayersEnabled()){
                    max = online + profile.getExtraPlayers();
                    ping.setPlayersMaximum(max);
                }
                
                serverPlaceholders = new ServerPlaceholders(online, max, hostAddresses.get(address.getHostString()));
                
                if(!profile.getMotd().isEmpty()){
                    Component component = ComponentParser.list(profile.getMotd())
                        .replacements(playerPlaceholders)
                        .replacements(serverPlaceholders)
                        .modifyText(text -> {
                            if(plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
                                return PlaceholderAPI.setPlaceholders(player.getPlayer(), text);
                            
                            return text;
                        })
                        .toComponent();
                    ping.setMotD(AdventureComponentConverter.fromComponent(component));
                }
                
                if(profile.shouldHidePlayers()){
                    ping.setPlayersVisible(false);
                }
                
                if(!profile.getPlayerCount().isEmpty() && !profile.shouldHidePlayers()){
                    ping.setVersionName(ComponentParser.text(profile.getPlayerCount())
                        .replacements(playerPlaceholders)
                        .replacements(serverPlaceholders)
                        .modifyText(text -> {
                            if(plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
                                return PlaceholderAPI.setPlaceholders(player.getPlayer(), text);
    
                            return text;
                        })
                        .toString()
                    );
                    ping.setVersionProtocol(-1);
                }
                
                if(!profile.getPlayers().isEmpty() && !profile.shouldHidePlayers()){
                    ping.setPlayers(
                        spigotPlugin.createPlayers(profile.getPlayers(), player.getPlayer(), playerPlaceholders, serverPlaceholders)
                    );
                }
                
                if(!profile.getFavicon().isEmpty()){
                    String favName = StringReplacer.replace(profile.getFavicon(), playerPlaceholders.getReplacements());
    
                    WrappedServerPing.CompressedImage favicon = spigotPlugin.getFaviconHandler().getFavicon(favName, image -> {
                        try{
                            return WrappedServerPing.CompressedImage.fromPng(image);
                        }catch(Exception ex){
                            return null;
                        }
                    });
                    
                    if(favicon == null){
                        spigotPlugin.getPluginLogger().warn("Could not obtain valid Favicon to use.");
                        ping.setFavicon(ping.getFavicon());
                    }else{
                        ping.setFavicon(favicon);
                    }
                }
            }
        });
    }
    
    private SpigotPlayer resolvePlayer(InetSocketAddress address, int protocol){
        String playerName = plugin.getCore().getPlayerHandler().getPlayerByIp(address.getHostString());
        OfflinePlayer player = Bukkit.getPlayerExact(playerName);
        
        if(player == null){
            //noinspection deprecation
            player = Bukkit.getOfflinePlayer(playerName);
            
            return new SpigotPlayer(player.hasPlayedBefore() ? player : null, playerName, protocol);
        }
        
        return new SpigotPlayer(player, playerName, protocol);
    }
}
