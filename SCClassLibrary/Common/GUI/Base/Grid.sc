DrawGrid {

	var <bounds,<>x,<>y;
	var <>opacity=0.7,<>smoothing=false,<>linePattern;

	*new { |bounds,horzGrid,vertGrid|
		^super.new.init(bounds, horzGrid, vertGrid)
	}
	*test { arg horzGrid,vertGrid,bounds;
		var w,grid;
		bounds = bounds ?? {Rect(0,0,500,400)};
		grid = DrawGrid(bounds,horzGrid,vertGrid);
		w = Window("Grid",bounds).front;
		UserView(w,bounds ?? {w.bounds.moveTo(0,0)})
			.resize_(5)
			.drawFunc_({ arg v;
				grid.bounds = v.bounds;
				grid.draw
			})
			.background_(Color.white)
		^grid
	}

	init { arg bounds,h,v;
		var w;
		x = DrawGridX(h);
		y = DrawGridY(v);
		this.bounds = bounds;
		this.font = Font( Font.defaultSansFace, 9 );
		this.fontColor = Color.grey(0.3);
		this.gridColors = [Color.grey(0.7),Color.grey(0.7)];
	}
	bounds_ { arg b;
		bounds = b;
		x.bounds = b;
		y.bounds = b;
	}
	draw {
		Pen.push;
			Pen.alpha = opacity;
			Pen.smoothing = smoothing;
			if(linePattern.notNil) {Pen.lineDash_(linePattern)};
			x.commands.do({ arg cmd; Pen.perform(cmd) });
			y.commands.do({ arg cmd; Pen.perform(cmd) });
		Pen.pop;
	}
	font_ { arg f;
		x.font = f;
		y.font = f;
	}
	fontColor_ { arg c;
		x.fontColor = c;
		y.fontColor = c;
	}
	gridColors_ { arg colors;
		x.gridColor = colors[0];
		y.gridColor = colors[1];
	}
	horzGrid_ { arg g;
		x.grid = g;
	}
	vertGrid_ { arg g;
		y.grid = g;
	}
	copy {
		^DrawGrid(bounds,x.grid,y.grid).x_(x.copy).y_(y.copy).opacity_(opacity).smoothing_(smoothing).linePattern_(linePattern)
	}
	clearCache {
		x.clearCache;
		y.clearCache;
	}
}


DrawGridX {

	var <grid,<>range,<>bounds;
	var <>font,<>fontColor,<>gridColor,<>labelOffset;
	var commands,cacheKey;
	var txtPad = 2; // match with Plot:txtPad

	*new { arg grid;
		^super.newCopyArgs(grid.asGrid).init
	}

	init {
		range = [grid.spec.minval, grid.spec.maxval];
		labelOffset = 4 @ -10;
	}
	grid_ { arg g;
		grid = g.asGrid;
		range = [grid.spec.minval, grid.spec.maxval];
		this.clearCache;
	}
	setZoom { arg min,max;
		range = [min,max];
	}
	commands {
		var p;
		var valNorm, lineColor;
		if(cacheKey != [range,bounds],{ commands = nil });
		^commands ?? {
			cacheKey = [range,bounds];
			commands = [];
			p = grid.getParams(range[0],range[1],bounds.left,bounds.right);

			p['lines'].do { arg val, i;
				var x;
				val = val.asArray; // value, [color]
				valNorm = grid.spec.unmap(val[0]);
				x = valNorm.linlin(0, 1, bounds.left, bounds.right);
				lineColor = val[1];

				commands = this.prAddLineCmds(commands, x, lineColor);

				// always draw line on left and right edges
				case
				{i == 0 and: { valNorm != 0 }} {
					commands = this.prAddLineCmds(commands, bounds.left, lineColor);
				}
				{i == (p['lines'].size-1) and: { valNorm != 1 }} {
					commands = this.prAddLineCmds(commands, bounds.right, lineColor);
				}
			};
			// Handle case where there is only one line:
			// left and middle line has been added, now need a right line
			if (p['lines'].size == 1 and: { valNorm != 1 }) {
				commands = this.prAddLineCmds(commands, bounds.right, lineColor);
			};

			if(p['labels'].notNil and: { labelOffset.x > 0 }, {
				commands = commands.add(['font_',font ] );
				commands = commands.add(['color_',fontColor ] );
				p['labels'].do { arg val; // value, label, [color, font]
					var x;
					if(val[2].notNil,{
						commands = commands.add( ['color_',val[2] ] );
					});
					if(val[3].notNil,{
						commands = commands.add( ['font_',val[3] ] );
					});
					x = grid.spec.unmap(val[0]).linlin(0, 1, bounds.left, bounds.right);

					commands = commands.add([
						'stringCenteredIn', val[1].asString,
						Rect.aboutPoint(
							x @ bounds.bottom, labelOffset.x/2, labelOffset.y/2
						).top_(bounds.bottom + txtPad)
					]);
				}
			});
			commands
		}
	}

	prAddLineCmds { |cmds, val, color|
		cmds = cmds.add( ['strokeColor_', color ? gridColor] );
		cmds = if (this.class == DrawGridX) {
			cmds.add( ['line', Point(val, bounds.top), Point(val, bounds.bottom) ] );
		} { // DrawGridY
			cmds.add( ['line', Point(bounds.left, val), Point(bounds.right, val) ] );
		};
		^cmds = cmds.add( ['stroke'] ); // return
	}

	clearCache { cacheKey = nil; }
	copy { ^super.copy.clearCache }
}


DrawGridY : DrawGridX {

	init {
		range = [grid.spec.minval, grid.spec.maxval];
		labelOffset = 4 @ 4;
	}
	commands {
		var p;
		var valNorm, lineColor;
		if(cacheKey != [range,bounds],{ commands = nil });

		^commands ?? {

			commands = [];

			p = grid.getParams(range[0], range[1], bounds.top, bounds.bottom);

			p['lines'].do { arg val, i; // value, [color]
				var y;
				val = val.asArray;
				valNorm = grid.spec.unmap(val[0]);
				lineColor = val[1];
				y = valNorm.linlin(0, 1, bounds.bottom, bounds.top);

				commands = this.prAddLineCmds(commands, y, lineColor);

				// draw grid line on top and bottom bound even if there is no 'line' there
				case
				{ i == 0 and: { valNorm != 0 } } { // bottom
					commands = this.prAddLineCmds(commands, bounds.bottom, lineColor);
				}
				{ i == (p['lines'].size-1) and: { valNorm != 1 } } { // top
					commands = this.prAddLineCmds(commands, bounds.top, lineColor);
				};
			};
			// Handle case where there is only one line:
			// bottom and middle line has been added, now need a top line
			if (p['lines'].size == 1 and: { valNorm != 1 }) {
				commands = this.prAddLineCmds(commands, bounds.top, lineColor);
			};

			if(p['labels'].notNil and: { labelOffset.y > 0 }, {
				commands = commands.add(['font_',font ] );
				commands = commands.add(['color_',fontColor ] );

				p['labels'].do { arg val;
					var y, lblRect;

					y = grid.spec.unmap(val[0]).linlin(0, 1 ,bounds.bottom, bounds.top);
					if(val[2].notNil,{
						commands = commands.add( ['color_', val[2]] );
					});
					if(val[3].notNil,{
						commands = commands.add( ['font_', val[3]] );
					});

					lblRect = Rect.aboutPoint(
						Point(labelOffset.x/2 - txtPad, y),
						labelOffset.x/2, labelOffset.y/2
					);

					switch(y.asInteger,
						bounds.bottom.asInteger, {
							lblRect = lblRect.bottom_(bounds.bottom + txtPad) },
						bounds.top.asInteger, {
							lblRect = lblRect.top_(bounds.top - txtPad) }
					);
					commands = commands.add(['stringRightJustIn', val[1].asString, lblRect]);
				}
			});
			commands
		}
	}
}

// DrawGridRadial : DrawGridX {}

GridLines {

	var <>spec;

	*new { arg spec;
		^super.newCopyArgs(spec.asSpec)
	}

	asGrid { ^this }
	niceNum { arg val,round;
		// http://books.google.de/books?id=fvA7zLEFWZgC&pg=PA61&lpg=PA61
		var exp,f,nf,rf;
		exp = floor(log10(val));
		f = val / 10.pow(exp);
		rf = 10.pow(exp);
		if(round,{
			if(f < 1.5,{
				^rf *  1.0
			});
			if(f < 3.0,{
				^rf *  2.0
			});
			if( f < 7.0,{
				^rf *  5.0
			});
			^rf *  10.0
		},{
			if(f <= 1.0,{
				^rf *  1.0;
			});
			if(f <= 2,{
				^rf *  2.0
			});
			if(f <= 5,{
				^rf *  5.0;
			});
			^rf *  10.0
		});
	}
	ideals { arg min,max,ntick=5;
		var nfrac,d,graphmin,graphmax,range,x;
		range = this.niceNum(max - min,false);
		d = this.niceNum(range / (ntick - 1),true);
		graphmin = floor(min / d) * d;
		graphmax = ceil(max / d) * d;
		nfrac = max( floor(log10(d)).neg, 0 );
		^[graphmin,graphmax,nfrac,d];
	}
	looseRange { arg min,max,ntick=5;
		^this.ideals(min,max).at( [ 0,1] )
	}
	getParams { |valueMin,valueMax,pixelMin,pixelMax,numTicks|
		var lines,p,pixRange;
		var nfrac,d,graphmin,graphmax,range;
		pixRange = pixelMax - pixelMin;
		if(numTicks.isNil,{
			numTicks = (pixRange / 64);
			numTicks = numTicks.max(3).round(1);
		});
		# graphmin,graphmax,nfrac,d = this.ideals(valueMin,valueMax,numTicks);
		lines = [];
		if(d != inf,{
			forBy(graphmin,graphmax + (0.5*d),d,{ arg tick;
				if(tick.inclusivelyBetween(valueMin,valueMax),{
					lines = lines.add( tick );
				})
			});
		});
		p = ();
		p['lines'] = lines;
		if(pixRange / numTicks > 9) {
			if (sum(lines % 1) == 0) { nfrac = 0 };
			p['labels'] = lines.collect({ arg val; [val, this.formatLabel(val,nfrac) ] });
		};
		^p
	}
	formatLabel { arg val, numDecimalPlaces;
		if (numDecimalPlaces == 0) {
			^val.asInteger.asString
		} {
			^val.round( (10**numDecimalPlaces).reciprocal).asString
		}
	}
}


BlankGridLines : GridLines {

	getParams {
		^()
	}
}


+ Nil {
	asGrid { ^BlankGridLines.new }
}
