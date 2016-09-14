package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.Pokeball;
import com.pokegoapi.api.inventory.Stats;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.util.Log;
import com.pokegoapi.util.Log.Level;
import com.pokegoapi.util.MapUtil;
import com.pokegoapi.util.PokeDictionary;

import POGOProtos.Inventory.Item.ItemAwardOuterClass.ItemAward;
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;

import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.player.PlayerProfile.Currency;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.settings.CatchOptions;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.EncounterFailedException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.NoSuchItemException;
import com.pokegoapi.exceptions.RemoteServerException;

import okhttp3.OkHttpClient;

public class Main {

	public static void main(String[] args) throws LoginFailedException, RemoteServerException, InterruptedException, NoSuchItemException, EncounterFailedException {
		// TODO Auto-generated method stub
		Log.setLevel(Level.NONE);
		OkHttpClient httpClient = new OkHttpClient();
		PokemonGo go = new PokemonGo(httpClient);
		
		System.out.println("Faça login pelo google (1) ou PTC (2)");
		
		Scanner sc = new Scanner(System.in);
		Integer opcao = sc.nextInt();
		Boolean logged = false;
		String username = null, password = null, access = null;
		GoogleUserCredentialProvider provider;
		
		if (opcao == 1) 
		{
			/** 
			* Google: 
			* You will need to redirect your user to GoogleUserCredentialProvider.LOGIN_URL
			* Afer this, the user must signin on google and get the token that will be show to him.
			* This token will need to be put as argument to login.
			*/
			provider = new GoogleUserCredentialProvider(httpClient);

			// in this url, you will get a code for the google account that is logged
			//System.out.println("Please go to " + GoogleUserCredentialProvider.LOGIN_URL);
			System.out.println("https://accounts.google.com/o/oauth2/auth?client_id=848232511240-73ri3t7plvk96pj4f85uj8otdat2alem.apps.googleusercontent.com&redirect_uri=urn%3Aietf%3Awg%3Aoauth%3A2.0%3Aoob&response_type=code&scope=openid%20email%20https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email");
			System.out.println("Enter authorization code:");

			// Ask the user to enter it in the standard input
			access = sc.next();
			
			System.out.println("Logando...");

			// we should be able to login with this token
			
			do {
			    try {
			    	provider.login(access);
					go.login(provider);
			        logged = true;
			    } catch(Exception e) {
			    	System.out.println("Erro! Tentando logar novamente...");
			    	sleepRandom(1000, 1500);
			    }
			} while(!logged);
			
			/**
			* After this, if you do not want to re-authorize the google account every time, 
			* you will need to store the refresh_token that you can get the first time with provider.getRefreshToken()
			* ! The API does not store the refresh token for you !
			* log in using the refresh token like this :
			*/
			//PokemonGo go = new PokemonGo(httpClient);
			//go.login(new GoogleUserCredentialProvider(httpClient, refreshToken));
		}
		else
		{
			/**
			* PTC is much simpler, but less secure.
			* You will need the username and password for each user log in
			* This account does not currently support a refresh_token. 
			* Example log in :
			*/
			System.out.println("Digite o usuário: ");
			username = sc.next();
			
			System.out.println("Digite a senha: ");
			password = sc.next();
			
			System.out.println("Logando...");
			
			do {
			    try {
			    	go.login(new PtcCredentialProvider(httpClient, username, password));
			        logged = true;
			    } catch(Exception e) {
			    	System.out.println("Erro! Tentando logar novamente...");
			    	sleepRandom(1000, 1500);
			    }
			} while(!logged);
		}
		
		// After this you can access the api from the PokemonGo instance :
		
		PlayerProfile pp = go.getPlayerProfile(); // to get the user profile
		Stats stats = pp.getStats();
		Inventories inventories = go.getInventories();
		int level = stats.getLevel();
		
		System.out.println("Nome: " + pp.getPlayerData().getUsername());
		System.out.println("Level: " + stats.getLevel());
		System.out.println("XP: " + stats.getExperience() + " (" 
				+ (stats.getNextLevelXp() - stats.getExperience()) + " to next level)");
		System.out.println("Team: " + pp.getPlayerData().getTeamValue());
		System.out.println("Stardust: " + pp.getCurrency(Currency.STARDUST));
		ArrayList<Point> pontos = new ArrayList<Point>();
		//ibirapuera
		pontos.add(new Point(-23.584289, -46.661537));
		pontos.add(new Point(-23.583952, -46.660532));
		pontos.add(new Point(-23.584045, -46.659832));
		pontos.add(new Point(-23.583830, -46.660822));
		pontos.add(new Point(-23.584547, -46.661737));
		//av cruzeiro do sul
//		pontos.add(new Point(-23.502127, -46.624735));
//		pontos.add(new Point(-23.512827, -46.625067));
		int cont = 0, distancia;
		double totalLat, totalLong, parteLat, parteLong;
		Map map = go.getMap();
		CatchResult cr;
		PokestopLootResult plr;
		CatchOptions catchOptions = new CatchOptions(go);
		catchOptions.maxPokeballs(3);
		catchOptions.noMasterBall(true);
		
		while (true) {
			if (cont + 1 != pontos.size())
			{
				totalLat = pontos.get(cont).getLatitude() - pontos.get(cont + 1).getLatitude();
				totalLong = pontos.get(cont).getLongitude() - pontos.get(cont + 1).getLongitude();
				distancia = (int) MapUtil.distFrom(pontos.get(cont), pontos.get(cont + 1));
			} else {
				totalLat = pontos.get(cont).getLatitude() - pontos.get(0).getLatitude();
				totalLong = pontos.get(cont).getLongitude() - pontos.get(0).getLongitude();
				distancia = (int) MapUtil.distFrom(pontos.get(cont), pontos.get(0));
				cont = 0;
			}
			parteLat = totalLat/distancia;
			parteLong = totalLong/distancia;
			for (int i = 0; i < distancia; i++)
			{
				try {
					go.setLocation(		 // set your position to get stuff around (altitude is not needed, you can use 1 for example)
							pontos.get(cont).getLatitude() - parteLat*i,
							pontos.get(cont).getLongitude() - parteLong*i,
							0
					);
					System.out.print(". ");
	
					for (Pokestop pokestop : map.getMapObjects().getPokestops()) // pega todas as pokestops
					{
						ArrayList<ItemAward> listaItensPokestop = new ArrayList<>();
						if (pokestop.canLoot()) 
						{
							plr = pokestop.loot();
							System.out.println("\n----------POKESTOP----------");
							System.out.println("EXP: " + plr.getExperience());
							for (ItemAward item :  plr.getItemsAwarded())
							{
								listaItensPokestop.add(item);
							}
							Set<ItemAward> unique = new HashSet<ItemAward>(listaItensPokestop);
							for (ItemAward key : unique)
							{
								System.out.println(getDisplayItemName(key.getItemId(), Locale.ENGLISH) + ": " + Collections.frequency(listaItensPokestop,  key));
							}
							inventories.updateInventories(true);
							sleepRandom(1000, 2000);
						}
						
					}
					
					ArrayList<Item> listaPotionRevives = new ArrayList<>();
					for (Item item : inventories.getItemBag().getItems()) //Deleta todas as potions e revives
					{
						if ((item.isPotion() || item.isRevive()) && (item.getCount() != 0))
						{
							listaPotionRevives.add(item);
						}					
					}
					
					if (!listaPotionRevives.isEmpty())
					{
						System.out.println("Removendo suas potions e revives...");
						for (int j = 0; j < listaPotionRevives.size(); j++)
						{
							inventories.getItemBag().removeItem(
									listaPotionRevives.get(j).getItemId(),
									listaPotionRevives.get(j).getCount());
							sleepRandom(1000, 2000);
						}
						inventories.updateInventories(true);
					}
					
					for (CatchablePokemon pokemon : map.getCatchablePokemon()) // get all currently Catchable Pokemon around you
					{
						pokemon.encounterPokemon();
						System.out.println("\n----POKEMON ENCONTRADO----");
						System.out.println("Capturando um " + PokeDictionary.getDisplayName(pokemon.getPokemonIdValue(), Locale.ENGLISH));
						if (pokemon.isEncountered())
						{
							Item pokeball = inventories.getItemBag().getItem(Pokeball.POKEBALL.getBallType());
							Item greatball = inventories.getItemBag().getItem(Pokeball.GREATBALL.getBallType());
							Item ultraball = inventories.getItemBag().getItem(Pokeball.ULTRABALL.getBallType());
							Item berry = inventories.getItemBag().getItem(ItemId.ITEM_RAZZ_BERRY);
							
							if (pokeball.getCount() > catchOptions.getMaxPokeballs() ||
								greatball.getCount() > catchOptions.getMaxPokeballs() ||
								ultraball.getCount() > catchOptions.getMaxPokeballs())
							{
								System.out.println("Chance de captura: " + pokemon.encounterPokemon().getCaptureProbability().getCaptureProbability(0));
								if (pokemon.encounterPokemon().getCaptureProbability().getCaptureProbability(0) > 0.5)
								{
									if (pokeball.getCount() > catchOptions.getMaxPokeballs())
									{
										catchOptions.usePokeball(Pokeball.POKEBALL);
										System.out.println("Usando Pokeball...");
									} 
									else if (greatball.getCount() > catchOptions.getMaxPokeballs())
									{
										catchOptions.usePokeball(Pokeball.GREATBALL);
										System.out.println("Usando Greatball...");
									}
									else if (ultraball.getCount() > catchOptions.getMaxPokeballs())
									{
										catchOptions.usePokeball(Pokeball.ULTRABALL);
										System.out.println("Usando Ultraball...");
									}
								} 
								else
								{
									if (ultraball.getCount() > catchOptions.getMaxPokeballs())
									{
										catchOptions.usePokeball(Pokeball.ULTRABALL);
										System.out.println("Usando Ultraball...");
									} 
									else if (greatball.getCount() > catchOptions.getMaxPokeballs())
									{
										catchOptions.usePokeball(Pokeball.GREATBALL);
										System.out.println("Usando Greatball...");
									}
									else if (pokeball.getCount() > catchOptions.getMaxPokeballs())
									{
										catchOptions.usePokeball(Pokeball.POKEBALL);
										System.out.println("Usando Pokeball...");
									}
									
									if (berry.getCount() > catchOptions.getMaxPokeballs())
									{
										catchOptions.useRazzberries(true);
										System.out.println("Usando Razzberry..");
									}
									else
									{
										catchOptions.useRazzberries(false);
									}
								}
								try {
									cr = pokemon.catchPokemon(catchOptions); //add CatchResult
									if (cr.isFailed())
									{
										System.out.println("A captura falhou.");
									} else {
										System.out.println("Pokemon capturado com sucesso!");
										System.out.println("Candies: " + cr.getCandyList().stream().mapToInt(Integer::intValue).sum());
										System.out.println("XP: " + cr.getXpList().stream().mapToInt(Integer::intValue).sum());
										System.out.println("Stardust: " + cr.getStardustList().stream().mapToInt(Integer::intValue).sum());
									}
									pp.updateProfile();
									stats = pp.getStats();
									inventories.updateInventories(true); 
									System.out.println("--------MEUS STATUS--------");
									System.out.println("Level: " + stats.getLevel());
									System.out.println("XP: " + stats.getExperience() + " (" + (stats.getNextLevelXp() - stats.getExperience()) + " to next level)");
								} catch (AsyncPokemonGoException e){
									System.out.println("Erro desconhecido.");
									//e.printStackTrace();
									logged = false;
									do {
									    try {
									    	System.out.println("Atualizando o token...");
									    	httpClient = new OkHttpClient();
											go = new PokemonGo(httpClient);
											if (opcao == 1)
											{
												provider = new GoogleUserCredentialProvider(httpClient);
												provider.login(access);
												go.login(provider);
												logged = true;
											} else {
												go.login(new PtcCredentialProvider(httpClient, username, password));
												logged = true;
											}
											go.getPlayerProfile(); // to get the user profile
											stats = pp.getStats();
											inventories = go.getInventories();
											map = go.getMap();
											catchOptions = new CatchOptions(go);
											catchOptions.maxPokeballs(3);
											catchOptions.noMasterBall(true);
									    } catch(Exception w) {
									    	w.printStackTrace();
									    	System.out.println("Erro! Tentando logar novamente...");
									    	sleepRandom(1000, 1500);
									    }
									} while(!logged);
								}
							}
							else
							{
								System.out.println("Você não tem pokebolas...");
							}
							sleepRandom(2000, 3000);
						}
					}

					ArrayList<EggIncubator> incubadores = new ArrayList<>();
					for (EggIncubator eggIncubator : inventories.getIncubators()) //Coloca os ovos para chocar
					{
						if (!eggIncubator.isInUse())
						{
							incubadores.add(eggIncubator);
						}
					}
					
					ArrayList<EggPokemon> eggs = new ArrayList<>();
					for(EggPokemon egg : inventories.getHatchery().getEggs())
					{
						if (!egg.isIncubate())
						{
							eggs.add(egg);
						}
					}
					if (!incubadores.isEmpty() && !eggs.isEmpty())
					{
						System.out.println("\nIncubando ovo(s)...");
						for (int j = 0; j < incubadores.size(); j++)
						{
							eggs.get(j).incubate(incubadores.get(j));
							sleepRandom(1000, 1500);
						}
					}
					
					ArrayList<Pokemon> pokemonLista = new ArrayList<>();
					ArrayList<Pokemon> evoluiLista = new ArrayList<>();
					ArrayList<Pokemon> expFarmLista = new ArrayList<>();
					
					for (Pokemon pokemon : inventories.getPokebank().getPokemons())
					{
						pokemonLista.add(pokemon);
					}
					
					for (Pokemon pokemon : pokemonLista)
					{				
						if (pokemon.getIvRatio() < 0.8 || pokemon.getLevel() < 10)
						{
							if (
									pokemon.getPokemonId().name().equals("PIDGEY") || 
									pokemon.getPokemonId().name().equals("WEEDLE") || 
									pokemon.getPokemonId().name().equals("CATERPIE")
								)
							{
								expFarmLista.add(pokemon);
							}
							else
							{
								System.out.println("\nTransferindo " + PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber(), Locale.ENGLISH) + "...");
								pokemon.transferPokemon();
								System.out.println("Pokemon transferido com sucesso!");
								inventories.updateInventories(true);
								sleepRandom(1000, 2000);
							}
						}
						else
						{
							evoluiLista.add(pokemon);
						}
					}
					
					Collections.sort(evoluiLista, new Comparator<Pokemon>(){
						public int compare(Pokemon p1, Pokemon p2) {
				            return Double.compare(p2.getIvRatio(), p1.getIvRatio()); // Ascending
				        }
					}); //ordena todos os pokemons por IV
					
					for (Pokemon pokemon : evoluiLista)
					{
						
						while (pokemon.canEvolve())
						{
							System.out.println("\nEvoluindo "
									+ PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber(), Locale.ENGLISH)
									+ " -> " 
									+ PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber() + 1, Locale.ENGLISH)
									+ "..."
							);
							pokemon = pokemon.evolve().getEvolvedPokemon();
							System.out.println("Pokemon evoluido com sucesso!");
							inventories.updateInventories(true);
							sleepRandom(1000, 2000);
						}
					}
					
					//if (expFarmLista.size() > 60) //implementar depois
					//{
						for (Pokemon pokemon : expFarmLista)
						{
							if (pokemon.canEvolve())
							{
								System.out.println("\nEvoluindo "
										+ PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber(), Locale.ENGLISH)
										+ " -> " 
										+ PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber() + 1, Locale.ENGLISH)
										+ "..."
								);
								pokemon = pokemon.evolve().getEvolvedPokemon();
								System.out.println("Pokemon evoluido com sucesso!");
								inventories.updateInventories(true);
								sleepRandom(1000, 2000);
							}
							System.out.println("\nTransferindo " + PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber(), Locale.ENGLISH) + "...");
							pokemon.transferPokemon();
							System.out.println("Pokemon transferido com sucesso!");
							inventories.updateInventories(true);
							sleepRandom(1000, 2000);
						}
					//}
						
					if (level < stats.getLevel())
					{
						level = stats.getLevel();
						System.out.println("Parabéns! Você agora é level " + level + "!");
						if (!pp.acceptLevelUpRewards(level).getRewards().isEmpty())
						{
							for (ItemAward key : pp.acceptLevelUpRewards(level).getRewards())
							{
								System.out.println(key.getItemId().name());
								System.out.println(getDisplayItemName(key.getItemId(), Locale.ENGLISH));
							}
							inventories.updateInventories(true);
							sleepRandom(1000, 2000);
						}
					}
					
					pp.checkAndEquipBadges();
					pp.updateProfile();
					
					sleepRandom(800, 1500);
				} catch (Exception e) {
					//e.printStackTrace();
					logged = false;
					do {
					    try {
					    	System.out.println("Atualizando o token...");
							if (opcao == 1)
							{
								provider = new GoogleUserCredentialProvider(httpClient);
								provider.login(access);
								go.login(provider);
								logged = true;
							} else {
								go.login(new PtcCredentialProvider(httpClient, username, password));
								logged = true;
							}
					    } catch(Exception w) {
					    	//w.printStackTrace();
					    	System.out.println("Erro! Tentando logar novamente...");
					    	sleepRandom(1000, 1500);
					    }
					} while(!logged);
				}
			}
			cont++;
		}
		
	}
	
	public static void sleepRandom(int minMilliseconds, int maxMilliseconds) {
		Random random = new Random(System.currentTimeMillis());
        int from = Math.max(minMilliseconds, maxMilliseconds);
        int to = Math.min(minMilliseconds, maxMilliseconds);
        try {
            int randomInt = random.nextInt((from - to) + 1) + to;
            //System.out.println("Esperando " + (randomInt / 1000.0F) + " segundos.");
            TimeUnit.MILLISECONDS.sleep(randomInt);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
		
	 public static String getDisplayItemName(ItemId id, Locale locale)
	 {
        switch(id)
        {
        case ITEM_POKE_BALL:
        return "Pokeball";
        case ITEM_GREAT_BALL:
            return "Greatball";
        case ITEM_ULTRA_BALL:
            return "Ultraball";
        case ITEM_MASTER_BALL:
            return "Masterball";
        case ITEM_POTION:
            return "Potion";
        case ITEM_SUPER_POTION:
            return "Super Potion";
        case ITEM_HYPER_POTION:
            return "Hyper Potion";
        case ITEM_REVIVE:
            return "Revive";
        case ITEM_MAX_REVIVE:
            return "Max Revive";
        case ITEM_RAZZ_BERRY:
            return "Razz Berry";
        case ITEM_BLUK_BERRY:
            return "Bluk Berry";
        case ITEM_NANAB_BERRY:
            return "Nanab Berry";
        case ITEM_PINAP_BERRY:
            return "Pinap Berry";
        case ITEM_WEPAR_BERRY:
            return "Wepar Berry";
        case ITEM_LUCKY_EGG:
            return "Lucky Egg";
        case ITEM_INCENSE_COOL:
            return "Incense Cool";
        case ITEM_INCENSE_FLORAL:
            return "Incense Floral";
        case ITEM_INCENSE_ORDINARY:
            return "Incense Ordinary";
        case ITEM_INCENSE_SPICY:
            return "Incense Spicy";
        case ITEM_INCUBATOR_BASIC:
            return "Incubator";
        case ITEM_INCUBATOR_BASIC_UNLIMITED:
            return "Incubator (Unlimited)";
        case ITEM_ITEM_STORAGE_UPGRADE:
            return "Storage Upgrade";
        case ITEM_SPECIAL_CAMERA:
            return "Camera";
        case ITEM_TROY_DISK:
            return "Troy Disk";
            default:
                return "Unknown Item";
        }
	 }
	        
}
