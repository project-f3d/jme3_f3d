package wf.frk.f3b.jme3.mergers.relations.linkers;

import static wf.frk.f3b.jme3.mergers.relations.LinkerHelpers.getRef1;
import static wf.frk.f3b.jme3.mergers.relations.LinkerHelpers.getRef2;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

import lombok.experimental.ExtensionMethod;
import lombok.extern.log4j.Log4j2;
import wf.frk.f3b.jme3.mergers.RelationsMerger;
import wf.frk.f3b.jme3.mergers.relations.Linker;
import wf.frk.f3b.jme3.mergers.relations.RefData;
@Log4j2
@ExtensionMethod({wf.frk.f3b.jme3.ext.jme3.AnimControlExt.class})
public class SkeletonToSpatial implements Linker{
	// see http://hub.jmonkeyengine.org/t/skeletoncontrol-or-animcontrol-to-host-skeleton/31478/4

	@Override
	public boolean doLink(RelationsMerger loader, RefData data) {
		Object op1=getRef1(data,Geometry.class);
		Object op2=getRef2(data,Skeleton.class);

		if(op1==null||op2==null){
			op2=getRef1(data,Skeleton.class);
			op1=getRef2(data,Node.class);
		}

		if(op1==null||op2==null) return false;
		Spatial v=(Spatial)op1;
		Skeleton sk=(Skeleton)op2;

		// TODO: update skel w/o remove
		v.removeControl(SkeletonControl.class);

		SkeletonControl skc=new SkeletonControl(sk);
		v.addControl(skc);

		Collection<AnimControl> controls=new LinkedList<AnimControl>();

		for(int i=0;i<v.getNumControls();i++){
			Control c=v.getControl(i);
			if( c instanceof AnimControl)controls.add((AnimControl)c);
		}

		boolean atLeastOne=false;
		for(AnimControl c:controls){
			atLeastOne=true;
			c.setSkeleton(sk);
		}

		// always add AnimControl else NPE when SkeletonControl.clone
		if(!atLeastOne)v.addControl(new AnimControl(sk));

		cloneMatWhenNeeded((Spatial)op1,data);
		return true;
	}



	private void cloneMatWhenNeeded(Spatial op2, final RefData data) {
		op2.depthFirstTraversal(new SceneGraphVisitor(){
			@Override
			public void visit(Spatial s) {
				if(s instanceof Geometry){
					Material m=((Geometry)s).getMaterial();
					String matref=data.context.idOf(m);
					if(matref==null){ // should never happen!
						log.error("Mat is not referred?");
						return;
					}
					String refusage="G~usage~"+matref;
					int n=(int)Optional.ofNullable(data.context.get(refusage)).orElse(0);
					if(n>1){
						Material clone=m.clone();
						data.context.put("G~"+matref+"~cloned~"+System.currentTimeMillis(),clone,matref);
						s.setMaterial(clone);
					}
				}
			}
		});
	}

}