package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.inventory.Item;
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

import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.player.PlayerProfile.Currency;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.settings.CatchOptions;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.NoSuchItemException;
import com.pokegoapi.exceptions.RemoteServerException;

import okhttp3.OkHttpClient;

public class Main {

	public static void main(String[] args) throws LoginFailedException, RemoteServerException, InterruptedException, NoSuchItemException {
		// TODO Auto-generated method stub
		Log.setLevel(Level.NONE);
		OkHttpClient httpClient = new OkHttpClient();
		PokemonGo go = new PokemonGo(httpClient);
		
		System.out.println("Faça login pelo google (1) ou PTC (2)");
		Scanner sc = new Scanner(System.in);
		Integer opcao = sc.nextInt();
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
			System.out.println("Please go to " + GoogleUserCredentialProvider.LOGIN_URL);
			System.out.println("Enter authorization code:");

			// Ask the user to enter it in the standard input
			access = sc.next();

			// we should be able to login with this token
			provider.login(access);
			access = provider.getRefreshToken();
			go.login(provider);
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
			
			go.login(new PtcCredentialProvider(httpClient, username, password));
		}
		
		int contLogin = 0;
		// After this you can access the api from the PokemonGo instance :
		PlayerProfile pp = go.getPlayerProfile(); // to get the user profile
		Stats stats = pp.getStats();
		Inventories inventories = go.getInventories();
		
		System.out.println("Nome: " + pp.getPlayerData().getUsername());
		System.out.println("Level: " + stats.getLevel());
		System.out.println("XP: " + stats.getExperience() + " (" 
				+ (stats.getNextLevelXp() - stats.getExperience()) + " to next level)");
		System.out.println("Team: " + pp.getPlayerData().getTeamValue());
		System.out.println("Stardust: " + pp.getCurrency(Currency.STARDUST));
		ArrayList<Point> pontos = new ArrayList<Point>();
		pontos.add(new Point(-23.584289, -46.661537));
		pontos.add(new Point(-23.583952, -46.660532));
		pontos.add(new Point(-23.584045, -46.659832));
		pontos.add(new Point(-23.58383, -46.660822));
		pontos.add(new Point(-23.584547, -46.661737));
		int cont = 0, distancia;
		double totalLat, totalLong, parteLat, parteLong;
		CatchOptions catchOptions = new CatchOptions(go);
		catchOptions.withProbability(0);
		catchOptions.maxPokeballs(3);
		catchOptions.useSmartSelect(true);
		catchOptions.useRazzberries(true);
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
				go.setLocation(		 // set your position to get stuff around (altitude is not needed, you can use 1 for example)
						pontos.get(0).getLatitude() - parteLat*i,
						pontos.get(0).getLongitude() - parteLong*i,
						0
				);
				//System.out.println(go.getLatitude() + ", " + go.getLongitude());
				System.out.print(". ");
				Map map = go.getMap();
				CatchResult cr;
				PokestopLootResult plr;
				
				for (Pokestop pokestop : map.getMapObjects().getPokestops()) // pega todas as pokestops
				{
					if (pokestop.canLoot()) 
					{
						plr = pokestop.loot();
						System.out.println("\n----------POKESTOP----------");
						System.out.println("EXP: " + plr.getExperience());
						System.out.println("Itens: ");
						for (ItemAward item :  plr.getItemsAwarded())
						{
							System.out.println(item.getItemId().name());
						}
						inventories.updateInventories(); 
					}
					
				}
				 
				for (CatchablePokemon pokemon : map.getCatchablePokemon()) // get all currently Catchable Pokemon around you
				{
					pokemon.encounterPokemon();
					System.out.println("\n----POKEMON ENCONTRADO----");
					System.out.println("Capturando um " + PokeDictionary.getDisplayName(pokemon.getPokemonIdValue(), Locale.ENGLISH));
					cr = pokemon.catchPokemon(catchOptions); //add CatchResult
					if (cr.isFailed())
					{
						System.out.println("A captura falhou.");
					} else {
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
					sleepRandom(2000, 3000);
				}
				
				for (EggIncubator eggIncubator : inventories.getIncubators()) //Coloca os ovos para chocar
				{
					if (!eggIncubator.isInUse())
					{
						for(EggPokemon egg : inventories.getHatchery().getEggs())
						{
							if (!egg.isIncubate())
							{
								System.out.println("Incubando um ovo...");
								egg.incubate(eggIncubator);
								inventories.updateInventories(true);
							}
						}
					}
				}
				
				for (Item item : inventories.getItemBag().getItems()) //Deleta todas as potions e revives
				{
					if ((item.isPotion() || item.isRevive()) && (item.getCount() != 0))
					{
						inventories.getItemBag().removeItem(item.getItemId(), item.getCount());
						System.out.println("Removendo suas potions e revives...");
						inventories.updateInventories(); 
						sleepRandom(1000, 2000);
					}
				}
				
				List<Pokemon> pokemonLista = inventories.getPokebank().getPokemons();
				int totalPokemon = pokemonLista.size();
				for (int j = 0; j < totalPokemon; j++)
				{
					Pokemon pokemon = inventories.getPokebank().getPokemons().get(j);
					
					if (pokemon.getIvRatio() < 0.8)
					{
						if (
								pokemon.getPokemonId().name().equals("PIDGEY") || 
								pokemon.getPokemonId().name().equals("WEEDLE") || 
								pokemon.getPokemonId().name().equals("CATERPIE") ||
								pokemon.getPokemonId().name().equals("ZUBAT") ||
								pokemon.getPokemonId().name().equals("RATTATA")
							)
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
						}
						System.out.println("\nTransferindo " + PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber(), Locale.ENGLISH) + "...");
						pokemon.transferPokemon();
						System.out.println("Pokemon transferido com sucesso!");
						inventories.updateInventories(true);
						totalPokemon--;
						sleepRandom(1000, 2000);
					}
					else
					{
						while (pokemon.canEvolve())
						{
							System.out.println("\nEvoluindo: " + PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber(), Locale.ENGLISH));
							System.out.println("Para: " + PokeDictionary.getDisplayName(pokemon.getPokemonId().getNumber() + 1, Locale.ENGLISH));
							pokemon = pokemon.evolve().getEvolvedPokemon();
							System.out.println("Pokemon evoluido com sucesso!");
							inventories.updateInventories(true);
							sleepRandom(1000, 2000);
						}
					}
				}
				contLogin++;
				if (contLogin == 100) //reseta a autorização a cada 3 minutos +/-
				{
					System.out.println("Atualizando o token...");
					if (opcao == 1)
					{
						go.login(new GoogleUserCredentialProvider(httpClient, access));
					} else {
						go.login(new PtcCredentialProvider(httpClient, username, password));
					}
					contLogin = 0;
				}
				sleepRandom(800, 1500);
			}
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
		
}
