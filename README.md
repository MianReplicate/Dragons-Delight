<img src="https://i.imgur.com/wrcKReQ.png">

# Dragon's Delight
**....is a Minecraft mod which dynamically adds diverse diets between Dragon Survival (and its addons) and other food mods like Farmer's Delight**

***This mod may be used in any modpack.***

**This mod only needs to be installed on the _SERVER_**

## 🤔 What mods are supported?
Any mod that adds dragons or foods are supported! This is not a datapack wrapper and is rather an actual mod that adds diets dynamically based on a few criteria.

## 🍴 How are diets calculated for each dragon? | Design Philosophy
Based on the default recipe for a food, we first determine what ingredients each dragon can eat based on their lore diet. Afterwards, we divide that by the amount of total ingredients involved in the recipe (not including utensils). Then the original nutrition and saturation values for the meal are multiplied by the quotient to get the final value for the diets.

For example, say we took a meal that included meat, lettuce, and tomatoes. The tundra dragon diet can eat meat but cannot eat lettuces or tomatoes. As such, they can eat 1/3 ingredients in the meal. If the default nutrition value for the meal was 6 for humans, then it would be 2 for the tundra dragon because it can eat 1/3 of the meal's ingredients.

If a quotient is so small that it ends up being 0, an alternative method is used to grab the nutrition and saturation values instead. This method is simple and just grabs all the nutrition and saturation values of all the ingredients in the food that the dragon can eat, and then sums them up for the final value.

## 👏 Credits
- Thanks to BlackAuresArt and vectorwing for creating Dragon Survival and Farmer's Delight!