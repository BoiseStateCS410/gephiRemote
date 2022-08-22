package erdplugin2;

/*
 * Derived from GridLayoutBuilder by M. Bastian
 * Additional code by Michael Lynott
 */

/*
Portions Copyrighted 2011 Gephi Consortium.
 */

import javax.swing.Icon;
import javax.swing.JPanel;

import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutUI;
import org.openide.util.lookup.ServiceProvider;

/**ERD1Layout: From Gravisto Sugiyama
 * 
 * @author Michael Lynott
 */

@ServiceProvider(service = LayoutBuilder.class)
public class ERD2LayoutBuilder implements LayoutBuilder {

//	@Override
	public String getName() {
		return "ERD1Layout";
	}

//	@Override
	public LayoutUI getUI() {
		return new LayoutUI() {

//			@Override
			public String getDescription() {
				return "";
			}

//			@Override
			public Icon getIcon() {
				return null;
			}

//			@Override
			public JPanel getSimplePanel(Layout layout) {
				return null;
			}

//			@Override
			public int getQualityRank() {
				return -1;
			}

//			@Override
			public int getSpeedRank() {
				return -1;
			}
		};
	}

//	@Override
	public Layout buildLayout() {
		return new ERD2Layout(this);
	}
}
