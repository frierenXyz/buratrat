-- Service: StarterPlayerScripts
-- Name: KeySystemClient

local Players = game:GetService("Players")
local UserInputService = game:GetService("UserInputService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local TweenService = game:GetService("TweenService")

local Player = Players.LocalPlayer
local PlayerGui = Player:WaitForChild("PlayerGui")

--------------------------------------------------------------------------------
-- 1. MOBILE DETECTION & KICK LOGIC
--------------------------------------------------------------------------------
local function checkDeviceAndEnforce()
	-- Heuristic: If Touch is enabled, but no Keyboard/Mouse, it's likely a mobile device.
	-- Note: Some laptops have touchscreens, so we check for KeyboardEnabled to spare them.
	local isMobile = UserInputService.TouchEnabled and not UserInputService.KeyboardEnabled
	
	if isMobile then
		Player:Kick("Not available on mobile as of now")
		return true -- Return true to stop script execution
	end
	return false
end

if checkDeviceAndEnforce() then return end -- Stop script if kicked

--------------------------------------------------------------------------------
-- 2. MODERN UI GENERATION (Procedural)
--------------------------------------------------------------------------------
local function createModernUI()
	local ScreenGui = Instance.new("ScreenGui")
	ScreenGui.Name = "KeySystemUI"
	ScreenGui.ResetOnSpawn = false
	ScreenGui.IgnoreGuiInset = true
	ScreenGui.Parent = PlayerGui

	-- Main Background Frame
	local MainFrame = Instance.new("Frame")
	MainFrame.Size = UDim2.new(0, 400, 0, 250)
	MainFrame.Position = UDim2.new(0.5, 0, 0.5, 0)
	MainFrame.AnchorPoint = Vector2.new(0.5, 0.5)
	MainFrame.BackgroundColor3 = Color3.fromRGB(25, 25, 25)
	MainFrame.BorderSizePixel = 0
	MainFrame.Parent = ScreenGui

	-- Modern Styling: Corner
	local UICorner = Instance.new("UICorner")
	UICorner.CornerRadius = UDim.new(0, 12)
	UICorner.Parent = MainFrame

	-- Modern Styling: Red Border (Requested)
	local UIStroke = Instance.new("UIStroke")
	UIStroke.Color = Color3.fromRGB(255, 0, 0) -- Red
	UIStroke.Thickness = 2
	UIStroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border
	UIStroke.Parent = MainFrame

	-- Title
	local TitleLabel = Instance.new("TextLabel")
	TitleLabel.Text = "SECURITY SYSTEM"
	TitleLabel.Font = Enum.Font.GothamBold
	TitleLabel.TextSize = 24
	TitleLabel.TextColor3 = Color3.fromRGB(255, 255, 255)
	TitleLabel.BackgroundTransparency = 1
	TitleLabel.Size = UDim2.new(1, 0, 0.2, 0)
	TitleLabel.Parent = MainFrame

	-- Input Box Container (for styling)
	local InputContainer = Instance.new("Frame")
	InputContainer.Size = UDim2.new(0.8, 0, 0.2, 0)
	InputContainer.Position = UDim2.new(0.1, 0, 0.35, 0)
	InputContainer.BackgroundColor3 = Color3.fromRGB(40, 40, 40)
	InputContainer.Parent = MainFrame
	
	local InputCorner = Instance.new("UICorner")
	InputCorner.CornerRadius = UDim.new(0, 8)
	InputCorner.Parent = InputContainer

	-- Actual TextBox
	local TextBox = Instance.new("TextBox")
	TextBox.Size = UDim2.new(1, -20, 1, 0)
	TextBox.Position = UDim2.new(0, 10, 0, 0)
	TextBox.BackgroundTransparency = 1
	TextBox.Text = ""
	TextBox.PlaceholderText = "Enter Key..."
	TextBox.TextColor3 = Color3.fromRGB(255, 255, 255)
	TextBox.PlaceholderColor3 = Color3.fromRGB(150, 150, 150)
	TextBox.Font = Enum.Font.Gotham
	TextBox.TextSize = 18
	TextBox.Parent = InputContainer

	-- Submit Button
	local SubmitBtn = Instance.new("TextButton")
	SubmitBtn.Size = UDim2.new(0.5, 0, 0.18, 0)
	SubmitBtn.Position = UDim2.new(0.25, 0, 0.7, 0)
	SubmitBtn.BackgroundColor3 = Color3.fromRGB(200, 0, 0) -- Red Button
	SubmitBtn.Text = "SUBMIT"
	SubmitBtn.Font = Enum.Font.GothamBold
	SubmitBtn.TextColor3 = Color3.fromRGB(255, 255, 255)
	SubmitBtn.TextSize = 16
	SubmitBtn.AutoButtonColor = false -- We handle animation manually
	SubmitBtn.Parent = MainFrame

	local BtnCorner = Instance.new("UICorner")
	BtnCorner.CornerRadius = UDim.new(0, 8)
	BtnCorner.Parent = SubmitBtn

	-- Feedback Label
	local FeedbackLabel = Instance.new("TextLabel")
	FeedbackLabel.Text = ""
	FeedbackLabel.Font = Enum.Font.Gotham
	FeedbackLabel.TextSize = 14
	FeedbackLabel.TextColor3 = Color3.fromRGB(255, 100, 100)
	FeedbackLabel.BackgroundTransparency = 1
	FeedbackLabel.Size = UDim2.new(1, 0, 0.1, 0)
	FeedbackLabel.Position = UDim2.new(0, 0, 0.9, 0)
	FeedbackLabel.Parent = MainFrame

	return ScreenGui, TextBox, SubmitBtn, FeedbackLabel, MainFrame
end

local UI, KeyInput, SubmitButton, Feedback, MainFrame = createModernUI()

--------------------------------------------------------------------------------
-- 3. INTERACTION LOGIC
--------------------------------------------------------------------------------
local ValidateRemote = ReplicatedStorage:WaitForChild("ValidateKey")
local debounce = false

local function animateButton(btn)
	local originalSize = UDim2.new(0.5, 0, 0.18, 0)
	local shrinkSize = UDim2.new(0.45, 0, 0.16, 0)
	
	local tweenInfo = TweenInfo.new(0.1, Enum.EasingStyle.Quad, Enum.EasingDirection.Out)
	
	TweenService:Create(btn, tweenInfo, {Size = shrinkSize}):Play()
	task.wait(0.1)
	TweenService:Create(btn, tweenInfo, {Size = originalSize}):Play()
end

local function onSubmit()
	if debounce then return end
	debounce = true
	
	animateButton(SubmitButton)
	Feedback.Text = "Checking..."
	Feedback.TextColor3 = Color3.fromRGB(255, 255, 255)

	local key = KeyInput.Text
	
	-- Security: Invoke server to check key
	local success = ValidateRemote:InvokeServer(key)
	
	if success then
		Feedback.Text = "Access Granted"
		Feedback.TextColor3 = Color3.fromRGB(0, 255, 0)
		KeyInput.TextEditable = false
		
		-- Success Animation (Fade out UI)
		task.wait(1)
		local fadeInfo = TweenInfo.new(0.5)
		TweenService:Create(MainFrame, fadeInfo, {Position = UDim2.new(0.5, 0, 1.5, 0)}):Play()
		task.wait(0.5)
		UI:Destroy()
		
		-- ENABLE GAMEPLAY HERE
		print("Key System Passed")
	else
		Feedback.Text = "Invalid Key"
		Feedback.TextColor3 = Color3.fromRGB(255, 0, 0)
		
		-- Shake Animation
		local basePos = MainFrame.Position
		for i = 1, 5 do
			MainFrame.Position = basePos + UDim2.new(0, math.random(-5, 5), 0, 0)
			task.wait(0.05)
		end
		MainFrame.Position = basePos
	end
	
	debounce = false
end

SubmitButton.MouseButton1Click:Connect(onSubmit)
