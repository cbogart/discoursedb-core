/*******************************************************************************
 * Copyright (C)  2015 - 2016  Carnegie Mellon University
 * Author: Oliver Ferschke
 *
 * This file is part of DiscourseDB.
 *
 * DiscourseDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * DiscourseDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscourseDB.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301  USA
 *******************************************************************************/
package edu.cmu.cs.lti.discoursedb.core.repository.macro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscourseToDiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.repository.BaseRepository;

public interface DiscourseToDiscoursePartRepository extends BaseRepository<DiscourseToDiscoursePart,Long>{
    
	Optional<DiscourseToDiscoursePart> findOneByDiscourseAndDiscoursePart(Discourse discourse, DiscoursePart discoursePart);
	List<DiscourseToDiscoursePart> findByDiscourse(Discourse discourse);

	@Query("select dp from DiscoursePart dp left join fetch dp.annotations aa "
			+ "left join fetch aa.annotations ai left join fetch ai.features feat "
			+ " where dp.type=:discoursePartType")
	List<DiscoursePart> findExtendedByType(@Param("discoursePartType") String discoursePartType);

	@Query("select ddp.discourse from DiscourseToDiscoursePart ddp where ddp.discoursePart = :discoursePart")
	List<Discourse> findDiscoursesOfDiscoursePart(@Param("discoursePart") DiscoursePart discoursePart);
}
