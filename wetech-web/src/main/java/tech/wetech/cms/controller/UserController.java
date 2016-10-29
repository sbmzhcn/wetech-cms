package tech.wetech.cms.controller;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import tech.wetech.cms.auth.AuthClass;
import tech.wetech.cms.auth.AuthMethod;
import tech.wetech.cms.dto.DataTableMap;
import tech.wetech.cms.dto.UserDto;
import tech.wetech.cms.model.ChannelTree;
import tech.wetech.cms.model.Role;
import tech.wetech.cms.model.RoleType;
import tech.wetech.cms.model.User;
import tech.wetech.cms.service.IChannelService;
import tech.wetech.cms.service.IGroupService;
import tech.wetech.cms.service.IRoleService;
import tech.wetech.cms.service.IUserService;

@Controller
@RequestMapping("/admin/user")
@AuthClass("login")
public class UserController {

	@Inject
	private IUserService userService;
	@Inject
	private IGroupService groupService;
	@Inject
	private IRoleService roleService;
	@Inject
	private IChannelService channelService;

	private void initUser(Model model) {
		model.addAttribute("roles", roleService.listRole());
		model.addAttribute("groups", groupService.listGroup());
	}
	
	@RequestMapping("/user")
	public String user(Model model) {
		initUser(model);
		return "user/user";
	}
	
	@RequestMapping("/list")
	public @ResponseBody Map<String,Object>  list() {
		return DataTableMap.getMapData(userService.findUser());
	}
	
	@RequestMapping("/users")
	public String list2(Model model) {
		model.addAttribute("datas", userService.findUser());
		return "user/list";
	}
	
	
	@Deprecated
	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public String add(Model model) {
		model.addAttribute("userDto", new UserDto());// user,user
		initUser(model);
		return "user/add";
	}

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public String add(@Valid UserDto userDto, BindingResult br, Model model) {
		if (br.hasErrors()) {
			initUser(model);
			return "user/add";
		}
		userService.add(userDto.getUser(), userDto.getRoleIds(), userDto.getGroupIds());
		return "redirect:/admin/user/users";
	}

	@RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
	public String update(@PathVariable int id, Model model) {
		User u = userService.load(id);
		model.addAttribute("userDto",
				new UserDto(u, userService.listUserRoleIds(id), userService.listUserGroupIds(id)));// user,user
		initUser(model);
		return "user/update";
	}

	@RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
	public String update(@PathVariable int id, @Valid UserDto userDto, BindingResult br, Model model) {
		if (br.hasErrors()) {
			System.out.println(br.hasErrors());
			initUser(model);
			return "user/update";
		}
		User ou = userService.load(id);
		ou.setNickname(userDto.getNickname());
		ou.setPhone(userDto.getPhone());
		ou.setEmail(userDto.getEmail());
		ou.setStatus(userDto.getStatus());
		userService.update(ou, userDto.getRoleIds(), userDto.getGroupIds());
		return "redirect:/admin/user/users";
	}

	@RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
	public String delete(@PathVariable int id) {
		userService.delete(id);
		return "redirect:/admin/user/users";
	}

	@RequestMapping(value = "/updateStatus/{id}", method = RequestMethod.GET)
	public String updateStatus(@PathVariable int id) {
		userService.updateStatus(id);
		return "redirect:/admin/user/users";
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public String show(@PathVariable int id, Model model) {
		model.addAttribute(userService.load(id));
		model.addAttribute("gs", userService.listUserGroups(id));
		model.addAttribute("rs", userService.listUserRoles(id));
		return "user/show";
	}

	@RequestMapping("/showSelf")
	@AuthMethod
	public String showSelf(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loginUser");
		model.addAttribute(user);
		model.addAttribute("gs", userService.listUserGroups(user.getId()));
		model.addAttribute("rs", userService.listUserRoles(user.getId()));
		return "user/show";
	}

	@RequestMapping(value = "/updatePwd", method = RequestMethod.GET)
	@AuthMethod
	public String updatePwd(Model model, HttpSession session) {
		User u = (User) session.getAttribute("loginUser");
		model.addAttribute(u);
		return "user/updatePwd";
	}

	@RequestMapping(value = "/updatePwd", method = RequestMethod.POST)
	@AuthMethod
	public String updatePwd(int id, String oldPwd, String password) {
		userService.updatePwd(id, oldPwd, password);
		return "redirect:/admin/user/showSelf";
	}

	/*
	 * @RequestMapping(value="/updatePwd",method=RequestMethod.POST) public
	 * String updatePwd(Model model,HttpSession session) { User u =
	 * (User)session.getAttribute("loginUser"); model.addAttribute(u); return
	 * "user/updatePwd"; }
	 */

	@RequestMapping(value = "/updateSelf", method = RequestMethod.GET)
	@AuthMethod
	public String updateSelf(Model model, HttpSession session) {
		User u = (User) session.getAttribute("loginUser");
		model.addAttribute(new UserDto(u));
		return "user/updateSelf";
	}

	@RequestMapping(value = "/updateSelf", method = RequestMethod.POST)
	@AuthMethod
	public String updateSelf(@Valid UserDto userDto, BindingResult br, Model model, HttpSession session) {
		if (br.hasErrors()) {
			return "user/updateSelf";
		}
		User ou = userService.load(userDto.getId());
		ou.setNickname(userDto.getNickname());
		ou.setPhone(userDto.getPhone());
		ou.setEmail(userDto.getEmail());
		userService.update(ou);
		session.setAttribute("loginUser", ou);
		return "redirect:/admin/user/showSelf";
	}

	@RequestMapping("/listChannels/{uid}")
	public String listChannels(@PathVariable int uid, Model model) {
		model.addAttribute(userService.load(uid));
		List<Role> rs = userService.listUserRoles(uid);
		for (Role r : rs) {
			if (r.getRoleType() == RoleType.ROLE_ADMIN) {
				model.addAttribute("uAdmin", 1);
			}
		}
		return "/user/listChannel";
	}

	@RequestMapping("/userTree/{uid}")
	public @ResponseBody List<ChannelTree> groupTree(@PathVariable Integer uid, @RequestParam Integer isAdmin) {
		if (isAdmin != null && isAdmin == 1) {
			return channelService.generateTree();
		}
		return groupService.generateUserChannelTree(uid);
	}

}