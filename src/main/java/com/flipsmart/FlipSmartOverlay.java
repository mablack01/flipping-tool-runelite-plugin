package com.flipsmart;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class FlipSmartOverlay extends OverlayPanel
{
	private final Client client;
	private final FlipSmartPlugin plugin;
	private final FlipSmartConfig config;

	@Inject
	private FlipSmartOverlay(Client client, FlipSmartPlugin plugin, FlipSmartConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		panelComponent.getChildren().clear();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Flip Smart")
			.color(Color.GREEN)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Status:")
			.right("Active")
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Min Profit:")
			.right(config.minimumProfit() + " GP")
			.build());

		return super.render(graphics);
	}
}

