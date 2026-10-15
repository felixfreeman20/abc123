# Auction HUD (Fabric 26.2, client-only)

## Get the .jar (no installs)
1. Make a free GitHub account, then create a new empty repo.
2. Unzip this, open the repo -> "Add file" -> "Upload files", drag in EVERYTHING from inside the folder (including the .github folder), commit.
3. Go to the repo's "Actions" tab. The "Build mod" run starts by itself (~2-3 min).
4. Open the finished run -> scroll to "Artifacts" -> download auctionhud-jar.zip -> unzip -> put the .jar in your mods folder
   (needs Fabric Loader 0.19.3+ and Fabric API for 26.2).

## Use
Key: H toggles the HUD (rebind in Controls).
/auction start [seconds] | stop | reset | timer <s> | min <amount> | ign <name>
/auction item add [id] | item remove <id> | item clear | display icon|text|both | pos <x> <y> | toggle
