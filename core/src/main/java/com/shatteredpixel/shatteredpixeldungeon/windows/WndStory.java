/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.utils.SparseArray;

public class WndStory extends Window {

	private static final int WIDTH_P = 125;
	private static final int WIDTH_L = 160;
	private static final int MARGIN = 2;
	
	public static final int ID_SEWERS		= 0;
	public static final int ID_PRISON		= 1;
	public static final int ID_CAVES		= 2;
	public static final int ID_CITY     	= 3;
	public static final int ID_HALLS		= 4;
	
	private static final SparseArray<String> CHAPTERS = new SparseArray<>();
	
	static {
		CHAPTERS.put( ID_SEWERS, "sewers" );
		CHAPTERS.put( ID_PRISON, "prison" );
		CHAPTERS.put( ID_CAVES, "caves" );
		CHAPTERS.put( ID_CITY, "city" );
		CHAPTERS.put( ID_HALLS, "halls" );
	}

	private IconTitle ttl;
	private RenderedTextBlock tf;
	
	private float delay;

	public WndStory( String text ) {
		this( null, null, text );
	}
	
	public WndStory(Image icon, String title, String text ) {
		super( 0, 0, Chrome.get( Chrome.Type.SCROLL ) );

		int width = PixelScene.landscape() ? WIDTH_L - MARGIN * 2: WIDTH_P - MARGIN *2;

		float y = MARGIN;
		if (icon != null && title != null){
			ttl = new IconTitle(icon, title);
			ttl.setRect(MARGIN, y, width-2*MARGIN, 0);
			y = ttl.bottom()+MARGIN;
			add(ttl);
			ttl.tfLabel.invert();
		}
		
		tf = PixelScene.renderTextBlock( text, 6 );
		tf.maxWidth(width);
		tf.invert();
		tf.setPos(MARGIN, y);
		add( tf );

		PointerArea blocker = new PointerArea( 0, 0, PixelScene.uiCamera.width, PixelScene.uiCamera.height ) {
			@Override
			protected void onClick( PointerEvent event ) {
				onBackPressed();
			}
		};
		blocker.setCamera(PixelScene.uiCamera);
		add(blocker);
		
		resize( (int)(tf.width() + MARGIN * 2), (int)Math.min( tf.bottom()+MARGIN, 180 ) );
	}
	
	@Override
	public void update() {
		super.update();
		
		if (delay > 0 && (delay -= Game.INSTANCE.elapsed) <= 0) {
			tf.setVisible(true);
			chrome.setVisible(tf.getVisible());
			shadow.setVisible(chrome.getVisible());
			if (ttl != null) ttl.setVisible(true);
		}
	}
	
	public static void showChapter( int id ) {
		
		if (Dungeon.chapters.contains( id )) {
			return;
		}
		
		String text = Messages.get(WndStory.class, CHAPTERS.get( id ));
		if (text != null) {
			WndStory wnd = new WndStory( text );
			if ((wnd.delay = 0.6f) > 0) {
				wnd.tf.setVisible(false);
				wnd.chrome.setVisible(wnd.tf.getVisible());
				wnd.shadow.setVisible(wnd.chrome.getVisible());
				if (wnd.ttl != null) wnd.ttl.setVisible(false);
			}
			
			Game.INSTANCE.scene.add( wnd );
			
			Dungeon.chapters.add( id );
		}
	}
}
